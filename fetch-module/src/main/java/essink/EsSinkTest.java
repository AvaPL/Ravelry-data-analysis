package essink;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Source;
import co.elastic.clients.base.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: Only to test the functionality, remove when not needed
public class EsSinkTest {
    public static void main(String[] args) {
        val restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
        val transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        val client = new ElasticsearchClient(transport);

        val objectMapper = new ObjectMapper();
        Iterable<JsonNode> documents = IntStream.range(0, 10)
                .mapToObj(i -> {
                    val json = objectMapper.createObjectNode();
                    json.put("key" + i, "value" + i);
                    return json;
                })
                .collect(Collectors.toList());

        val esSink = new EsSink(client, "my-index");
        val system = ActorSystem.create();
        Source.single(documents).to(esSink.create()).run(system);
    }
}
