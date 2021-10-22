package essink;

import akka.Done;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._core.BulkRequest;
import co.elastic.clients.elasticsearch._core.BulkResponse;
import co.elastic.clients.elasticsearch._core.bulk.Operation;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@AllArgsConstructor
public class EsSink {

    private ElasticsearchClient client; // TODO: Use async client
    private String index;

    public Sink<Iterable<JsonNode>, CompletionStage<Done>> create() { // TODO: Add exception handling
        return Flow.<Iterable<JsonNode>>create()
                .map(this::indexDocuments)
                .toMat(printResponseItems(), Keep.right());
    }

    private BulkResponse indexDocuments(Iterable<JsonNode> documents) throws IOException {
        return client.<JsonNode>bulk(builder ->
                indexDocumentsBuilder(builder, documents)
        );
    }

    private BulkRequest.Builder<JsonNode> indexDocumentsBuilder(
            BulkRequest.Builder<JsonNode> builder,
            Iterable<JsonNode> documents
    ) {
        for (JsonNode document : documents) {
            builder
                    .addOperation(EsSink::indexOperation)
                    .index(index).addDocument(document);
        }
        return builder;
    }

    private static Operation.Builder indexOperation(Operation.Builder builder) {
        return builder.index(x -> x);
    }

    private Sink<BulkResponse, CompletionStage<Done>> printResponseItems() {
        return Flow.of(BulkResponse.class)
                .flatMapConcat(response -> Source.from(responseItems(response)))
                .toMat(Sink.foreach(this::printIndexedItem), Keep.right());
    }

    private List<String> responseItems(BulkResponse response) {
        return response
                .items()
                .stream()
                .map(item -> item.index().id())
                .collect(Collectors.toList());
    }

    private void printIndexedItem(String id) {
        System.out.println("Indexed document [id: " + id + ", index: " + index + "]");
    }
}
