package graphs;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.*;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import com.fasterxml.jackson.databind.JsonNode;
import esflow.EsFlow;
import lombok.val;
import okhttp3.OkHttpClient;
import pageinfosource.PageInfoSource;
import util.ParseJsonFunctions;
import pojo.PageInfo;
import raverlyapicallflow.RavelryApiCallFlow;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class PatternsGraph {
    public static RunnableGraph<CompletionStage<Done>> create(
            OkHttpClient apiClient,
            ElasticsearchAsyncClient esClient
    ) {
        val pageInfoSource = getPageInfoSource();
        val fetchPatternsFlow = getFetchPatternsFlow(apiClient);
        val extractEntitiesFlow = getExtractEntitiesFlow();
        val patternsToEsFlow = getPatternsToEsFlow(esClient);
        val ignoreSink = Sink.<Iterable<JsonNode>>ignore();

        return pageInfoSource
                .via(fetchPatternsFlow)
                .via(extractEntitiesFlow)
                .via(patternsToEsFlow)
                .toMat(ignoreSink, Keep.right());
    }

    private static Source<PageInfo, NotUsed> getPageInfoSource() {
        return new PageInfoSource(100, 10).create();
    }

    private static Flow<PageInfo, JsonNode, NotUsed> getFetchPatternsFlow(OkHttpClient apiClient) {
        return new RavelryApiCallFlow<>(apiClient, "/patterns/search.json", PatternsGraph::pageInfoToParams).create();
    }

    private static Map<String, String> pageInfoToParams(PageInfo pageInfo) {
        val params = new HashMap<String, String>();
        params.put("page", pageInfo.getPageNumber().toString());
        params.put("page_size", pageInfo.getPageLimit().toString());
        return params;
    }

    private static Flow<JsonNode, Iterable<JsonNode>, NotUsed> getExtractEntitiesFlow() {
        return Flow.of(JsonNode.class)
                .map(ParseJsonFunctions.parseJsonNodeToJsonNodes("patterns"));
    }

    private static Flow<Iterable<JsonNode>, Iterable<JsonNode>, NotUsed> getPatternsToEsFlow(ElasticsearchAsyncClient esClient) {
        return new EsFlow(esClient, "patterns", jsonNode -> jsonNode.get("id").asText()).create();
    }
}
