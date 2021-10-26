package graphs;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.*;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import com.fasterxml.jackson.databind.JsonNode;
import esflow.EsFlow;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import okhttp3.OkHttpClient;
import pageinfosource.PageInfoSource;
import pojo.PageInfo;
import raverlyapicallflow.RavelryApiCallFlow;
import scala.PartialFunction;
import util.ParseJsonFunctions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Log4j2
public class PatternsAndYarnsGraph {
    public static RunnableGraph<CompletionStage<Done>> create(
            OkHttpClient apiClient,
            ElasticsearchAsyncClient esClient
    ) {
        val pageInfoSource = getPageInfoSource();
        val fetchPatternsFlow = getFetchPatternsFlow(apiClient);
        val extractPatternIdsFlow = getExtractPatternIdsFlow();
        val fetchPatternDetailsFlow = getFetchPatternDetailsFlow(apiClient);
        val extractPatternEntitiesFlow = getExtractEntitiesFlow("patterns");
        val patternsToEsFlow = getEntitiesToEsFlow(esClient, "patterns");
        val extractYarnIdsFlow = getExtractYarnIdsFlow();
        val fetchYarnsFlow = getFetchYarnsFlow(apiClient);
        val extractYarnEntitiesFlow = getExtractEntitiesFlow("yarns");
        val yarnsToEsFlow = getEntitiesToEsFlow(esClient, "yarns");
        val ignoreSink = Sink.<Collection<JsonNode>>ignore();

        return pageInfoSource
                .via(fetchPatternsFlow)
                .via(extractPatternIdsFlow)
                .via(fetchPatternDetailsFlow)
                .via(extractPatternEntitiesFlow)
                .via(patternsToEsFlow)
                .via(extractYarnIdsFlow)
                .via(fetchYarnsFlow)
                .via(extractYarnEntitiesFlow)
                .via(yarnsToEsFlow)
                .recover(logExceptions())
                .toMat(ignoreSink, Keep.right());
    }

    private static Source<PageInfo, NotUsed> getPageInfoSource() {
        return new PageInfoSource(100, 10).create();
    }

    private static Flow<PageInfo, JsonNode, NotUsed> getFetchPatternsFlow(OkHttpClient apiClient) {
        return new RavelryApiCallFlow<>(apiClient, "/patterns/search.json", PatternsAndYarnsGraph::pageInfoToParams)
                .create();
    }

    private static Map<String, String> pageInfoToParams(PageInfo pageInfo) {
        return Map.of(
                "page", pageInfo.getPageNumber().toString(),
                "page_size", pageInfo.getPageLimit().toString()
        );
    }

    private static Flow<JsonNode, Collection<Integer>, NotUsed> getExtractPatternIdsFlow() {
        return Flow.of(JsonNode.class)
                .map(ParseJsonFunctions.parseJsonNodeToIds("patterns"));
    }

    private static Flow<Collection<Integer>, JsonNode, NotUsed> getFetchPatternDetailsFlow(OkHttpClient apiClient) {
        val flow = new RavelryApiCallFlow<>(apiClient, "/patterns.json", PatternsAndYarnsGraph::idsToParams).create();
        return Flow.<Collection<Integer>>create()
                .filter(c -> !c.isEmpty())
                .via(flow);
    }

    private static Map<String, String> idsToParams(Collection<Integer> ids) {
        val joinedIds = ids.stream().map(id -> Integer.toString(id)).collect(Collectors.joining(" "));
        return Map.of("ids", joinedIds);
    }

    private static Flow<JsonNode, Collection<JsonNode>, NotUsed> getExtractEntitiesFlow(String nodesListName) {
        return Flow.of(JsonNode.class)
                .map(ParseJsonFunctions.parseJsonNodeToJsonNodes(nodesListName));
    }

    private static Flow<Collection<JsonNode>, Collection<JsonNode>, NotUsed> getEntitiesToEsFlow(ElasticsearchAsyncClient esClient, String index) {
        return new EsFlow(esClient, index, jsonNode -> jsonNode.get("id").asText()).create();
    }

    private static Flow<Collection<JsonNode>, Collection<Integer>, NotUsed> getExtractYarnIdsFlow() {
        return Flow.<Collection<JsonNode>>create()
                .map(parseJsonNodesToYarnIds());
    }

    static Function<Collection<JsonNode>, Collection<Integer>> parseJsonNodesToYarnIds() {
        return jsonNodes ->
                jsonNodes.stream()
                        .flatMap(node -> packs(node).stream())
                        .map(node -> node.get("yarn_id"))
                        .filter(Objects::nonNull)
                        .map(JsonNode::asInt)
                        .collect(Collectors.toSet());
    }

    @SneakyThrows
    private static Collection<JsonNode> packs(JsonNode node) {
        return ParseJsonFunctions.parseJsonNodeToJsonNodes("packs").apply(node);
    }

    private static Flow<Collection<Integer>, JsonNode, NotUsed> getFetchYarnsFlow(OkHttpClient apiClient) {
        val flow = new RavelryApiCallFlow<>(apiClient, "/yarns.json", PatternsAndYarnsGraph::idsToParams).create();
        return Flow.<Collection<Integer>>create()
                .filter(c -> !c.isEmpty())
                .via(flow);
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
