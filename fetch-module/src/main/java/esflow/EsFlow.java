package esflow;

import akka.NotUsed;
import akka.stream.RestartSettings;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RestartFlow;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._core.BulkRequest;
import co.elastic.clients.elasticsearch._core.BulkResponse;
import co.elastic.clients.elasticsearch._core.bulk.Operation;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Log4j2
public class EsFlow {

    private ElasticsearchAsyncClient client;
    private String index;
    private Function<JsonNode, String> idExtractor;

    public Flow<Iterable<JsonNode>, Iterable<JsonNode>, NotUsed> create() {
        return Flow.<Iterable<JsonNode>>create()
                .mapAsync(1, this::indexDocumentsSafe);
    }

    private CompletableFuture<Iterable<JsonNode>> indexDocumentsSafe(Iterable<JsonNode> documents) {
        val retryPolicy = getRetryPolicy();
        val fallback = getFallback(documents);
        return Failsafe.with(fallback, retryPolicy).getStageAsync(() -> indexDocuments(documents));
    }

    private RetryPolicy<Iterable<JsonNode>> getRetryPolicy() {
        return new RetryPolicy<Iterable<JsonNode>>()
                .withBackoff(3, 30, ChronoUnit.SECONDS)
                .withMaxRetries(5)
                .onFailedAttempt(e -> log.error("Failed bulk index [index: " + index + "]", e.getLastFailure()))
                .onRetry(e -> log.info("Retrying bulk index [index: " + index + "]"))
                .onRetriesExceeded(e -> log.error("Exceeded retries for bulk index [index: " + index + "]"));
    }

    private Fallback<Iterable<JsonNode>> getFallback(Iterable<JsonNode> documents) {
        return Fallback.of(documents) // pass documents downstream despite failing to index them
                .onSuccess(e -> log.debug("Passing documents downstream"));
    }

    private CompletableFuture<Iterable<JsonNode>> indexDocuments(Iterable<JsonNode> documents) throws IOException {
        if (Iterables.isEmpty(documents))
            return CompletableFuture.completedFuture(documents);
        return client.<JsonNode>bulk(builder ->
                indexDocumentsBuilder(builder, documents)
        ).thenApply(response -> {
            printResponseItems(response);
            return documents;
        });
    }

    private BulkRequest.Builder<JsonNode> indexDocumentsBuilder(
            BulkRequest.Builder<JsonNode> builder,
            Iterable<JsonNode> documents
    ) {
        for (JsonNode document : documents) {
            builder
                    .addOperation(operationBuilder -> indexOperation(operationBuilder, document))
                    .index(index).addDocument(document);
        }
        return builder;
    }

    private Operation.Builder indexOperation(Operation.Builder operationBuilder, JsonNode document) {
        return operationBuilder.index(indexBuilder ->
                indexBuilder.id(idExtractor.apply(document))
        );
    }

    private void printResponseItems(BulkResponse bulkResponse) {
        responseItemsIds(bulkResponse)
                .forEach(id ->
                        log.info("Indexed document [id: " + id + ", index: " + index + "]")
                );
    }

    private List<String> responseItemsIds(BulkResponse response) {
        return response
                .items()
                .stream()
                .map(item -> item.index().id())
                .collect(Collectors.toList());
    }
}
