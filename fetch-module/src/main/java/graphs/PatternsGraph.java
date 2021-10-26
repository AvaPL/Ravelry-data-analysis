package graphs;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function;
import akka.stream.javadsl.*;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import com.fasterxml.jackson.databind.JsonNode;
import esflow.EsFlow;
import lombok.val;
import okhttp3.OkHttpClient;
import pageinfosource.PageInfoSource;
import pojo.PageInfo;
import raverlyapicallflow.RavelryApiCallFlow;
import util.ParseJsonFunctions;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class PatternsGraph {
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
                .toMat(ignoreSink, Keep.right());
    }

    private static Source<PageInfo, NotUsed> getPageInfoSource() {
        return new PageInfoSource(100, 10).create();
    }

    private static Flow<PageInfo, JsonNode, NotUsed> getFetchPatternsFlow(OkHttpClient apiClient) {
        return new RavelryApiCallFlow<>(apiClient, "/patterns/search.json", PatternsGraph::pageInfoToParams).create();
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
        val flow = new RavelryApiCallFlow<>(apiClient, "/patterns.json", PatternsGraph::idsToParams).create();
        return Flow.<Collection<Integer>>create()
                .filter(c -> !c.isEmpty())
                .via(flow);
    }

    private static Map<String, String> idsToParams(Collection<Integer> ids) {
        val joinedIds = ids.stream().map(id -> Integer.toString(id)).collect(Collectors.joining("+"));
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

    private static Function<Collection<JsonNode>, Collection<Integer>> parseJsonNodesToYarnIds() {
        return jsonNodes -> {
            Set<Integer> yarnIds = new HashSet<>();
            for (JsonNode jsonNode : jsonNodes) {
                List<JsonNode> yarns = (List<JsonNode>) ParseJsonFunctions.parseJsonNodeToJsonNodes("packs").apply(jsonNode);
                yarns.forEach(jsonNode1 -> yarnIds.add(jsonNode1.get("yarn_id").asInt()));
            }
            return yarnIds;
        };
    }

    private static Flow<Collection<Integer>, JsonNode, NotUsed> getFetchYarnsFlow(OkHttpClient apiClient) {
        val flow = new RavelryApiCallFlow<>(apiClient, "/yarns.json", PatternsGraph::idsToParams).create();
        return Flow.<Collection<Integer>>create()
                .filter(c -> !c.isEmpty())
                .via(flow);
    }
}