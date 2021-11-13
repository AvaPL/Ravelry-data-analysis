package graphs;

import akka.Done;
import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.ClosedShape;
import akka.stream.javadsl.*;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.indices.CreateResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import esflow.EsFlow;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import okhttp3.OkHttpClient;
import pageinfosource.PageInfoSource;
import pojo.PageInfo;
import raverlyapicallflow.RavelryApiCallFlow;
import scala.PartialFunction;
import scala.jdk.javaapi.FutureConverters;
import util.ParseJsonFunctions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Log4j2
public class ShopsGraph {

    private static final int replicationFactor = 8;
    public static final String index = "shops";
    public static final String geopointField = "geopoint";

    public static RunnableGraph<CompletionStage<Done>> create(
            OkHttpClient apiClient,
            ElasticsearchAsyncClient esClient
    ) {
        val flow = getFlow(apiClient, esClient);
        return buildGraph(flow);
    }

    private static Flow<PageInfo, Collection<JsonNode>, NotUsed> getFlow(
            OkHttpClient apiClient,
            ElasticsearchAsyncClient esClient
    ) {
        val fetchShopsFlow = getFetchShopsFlow(apiClient);
        val extractShopEntitiesFlow = getExtractEntitiesFlow(index);
        val addGeopointFieldFlow = getAddGeopointFieldFlow();
        val shopsToEsFlow = getEntitiesToEsFlow(esClient, index);

        return fetchShopsFlow
                .async()
                .via(extractShopEntitiesFlow)
                .via(addGeopointFieldFlow)
                .via(shopsToEsFlow)
                .recover(logExceptions());
    }

    private static RunnableGraph<CompletionStage<Done>> buildGraph(Flow<PageInfo, Collection<JsonNode>, NotUsed> flow) {
        return RunnableGraph.fromGraph(
                GraphDSL.create(
                        Sink.<Collection<JsonNode>>ignore(),
                        (b, sink) -> {
                            val source = getPageInfoSource();
                            val balance = b.add(Balance.<PageInfo>create(replicationFactor));
                            val merge = b.add(Merge.<Collection<JsonNode>>create(replicationFactor));

                            b.from(b.add(source)).viaFanOut(balance);
                            for (int i = 0; i < replicationFactor; i++)
                                b.from(balance).via(b.add(flow)).toFanIn(merge);
                            b.from(merge).to(sink);

                            return ClosedShape.getInstance();
                        }
                )
        );
    }

    private static Source<PageInfo, NotUsed> getPageInfoSource() {
        return new PageInfoSource(100, 25).create();
    }

    private static Flow<PageInfo, JsonNode, NotUsed> getFetchShopsFlow(OkHttpClient apiClient) {
        return new RavelryApiCallFlow<>(apiClient, "/shops/search.json", ShopsGraph::pageInfoToParams)
                .create();
    }

    private static Map<String, String> pageInfoToParams(PageInfo pageInfo) {
        return Map.of(
                "page", pageInfo.getPageNumber().toString(),
                "page_size", pageInfo.getPageLimit().toString()
        );
    }

    private static Flow<JsonNode, Collection<JsonNode>, NotUsed> getExtractEntitiesFlow(String nodesListName) {
        return Flow.of(JsonNode.class)
                .map(ParseJsonFunctions.parseJsonNodeToJsonNodes(nodesListName));
    }

    private static Flow<Collection<JsonNode>, Collection<JsonNode>, NotUsed> getAddGeopointFieldFlow() {
        return Flow.<Collection<JsonNode>>create().map(ShopsGraph::addGeopointFields);
    }

    private static Collection<JsonNode> addGeopointFields(Collection<JsonNode> nodes) {
        return nodes.stream()
                .map(ShopsGraph::addGeopointField)
                .collect(Collectors.toList());
    }

    private static JsonNode addGeopointField(JsonNode node) {
        val longitude = node.get("longitude").asText();
        val latitude = node.get("latitude").asText();
        val mapper = new ObjectMapper();
        val geopoint = mapper.createObjectNode()
                .put("lon", longitude)
                .put("lat", latitude);
        return ((ObjectNode) node).set(geopointField, geopoint);
    }

    private static Flow<Collection<JsonNode>, Collection<JsonNode>, NotUsed> getEntitiesToEsFlow(ElasticsearchAsyncClient esClient, String index) {
        return new EsFlow(esClient, index, jsonNode -> jsonNode.get("id").asText()).create();
    }

    private static PartialFunction<Throwable, Collection<JsonNode>> logExceptions() {
        return new PFBuilder<Throwable, Collection<JsonNode>>()
                .match(RuntimeException.class, throwable -> {
                    log.error("Graph failed", throwable);
                    return Collections.emptyList();
                })
                .build();
    }
}
