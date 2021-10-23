package essink;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._core.BulkRequest;
import co.elastic.clients.elasticsearch._core.BulkResponse;
import co.elastic.clients.elasticsearch._core.bulk.Operation;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO: Add exception handling
@Log4j2
@AllArgsConstructor
public class EsFlow {

    private ElasticsearchAsyncClient client;
    private String index;
    private Function<JsonNode, String> idExtractor;

    public Flow<Iterable<JsonNode>, Iterable<JsonNode>, NotUsed> create() {
        return Flow.<Iterable<JsonNode>>create()
                .mapAsync(1, this::indexDocuments);
    }

    private CompletableFuture<Iterable<JsonNode>> indexDocuments(Iterable<JsonNode> documents) throws IOException {
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
