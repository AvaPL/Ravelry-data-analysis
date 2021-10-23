package esflow;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import co.elastic.clients.base.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: Only to test the functionality, remove when not needed
public class EsFlowTest {
    public static void main(String[] args) {
        val restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
        val transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        val client = new ElasticsearchAsyncClient(transport);

        val objectMapper = new ObjectMapper();
        Iterable<JsonNode> documents = IntStream.range(0, 10)
                .mapToObj(i -> {
                    val json = objectMapper.createObjectNode();
                    json.put("id", i);
                    json.put("key" + i, "value" + i);
                    return json;
                })
                .collect(Collectors.toList());

        val esFlow = new EsFlow(client, "my-index", document -> document.get("id").asText());
        val system = ActorSystem.create();
        Source.single(documents).via(esFlow.create()).to(Sink.ignore()).run(system);
    }
}
