package graphs;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.*;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import esflow.EsFlow;
import lombok.val;
import pageinfosource.PageInfoSource;
import pojo.PageInfo;

import java.util.Collections;
import java.util.concurrent.CompletionStage;

public class PatternsGraph {
    public static RunnableGraph<CompletionStage<Done>> create(ElasticsearchAsyncClient esClient) {
        val pageInfoSource = getPageInfoSource();
        val fetchPatternsFlow = getFetchPatternsFlow(); // Flow that converts PageInfo to JsonNode via HTTP call
        val extractEntitiesFlow = getExtractEntitiesFlow(); // Flow that converts JsonNode to
        // Iterable<JsonNode>
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

    private static Flow<PageInfo, JsonNode, NotUsed> getFetchPatternsFlow() {
        // TODO: Mock only, replace with concrete flow
        return Flow.of(PageInfo.class)
                .map(pageInfo -> {
                    val objectMapper = new ObjectMapper();
                    return objectMapper.createObjectNode();
                });
    }

    private static Flow<JsonNode, Iterable<JsonNode>, NotUsed> getExtractEntitiesFlow() {
        // TODO: Mock only, replace with concrete flow
        return Flow.of(JsonNode.class)
                .map(jsonNode -> Collections.emptyList());
    }

    private static Flow<Iterable<JsonNode>, Iterable<JsonNode>, NotUsed> getPatternsToEsFlow(ElasticsearchAsyncClient esClient) {
        return new EsFlow(esClient, "patterns", jsonNode -> jsonNode.get("id").asText()).create();
    }
}
