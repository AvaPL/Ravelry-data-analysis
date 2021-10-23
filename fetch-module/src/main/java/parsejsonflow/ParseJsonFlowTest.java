package parsejsonflow;

import akka.actor.ActorSystem;
import akka.japi.function.Function;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

public class ParseJsonFlowTest {

    // Function for parsing a single json node to an iterable of json nodes. To be used later in main.
    public static Function<JsonNode, Iterable<JsonNode>> parseJsonNodes() {
        return jsonNode -> {
            ArrayNode arrayNode = (ArrayNode) jsonNode.get("patterns");
            List<JsonNode> jsonNodeList = new ArrayList<>();
            arrayNode.forEach(jsonNodeList::add);
            System.out.println(jsonNodeList);
            return jsonNodeList;
        };
    }

    // Main for testing the aforementioned parsing function.
    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNodeExample = mapper.readTree("""
                {
                    "patterns": [
                        {
                            "free": false,
                            "id": 1168670,
                            "name": "Shawlography: Westknits MKAL 2021",
                            "first_photo": {
                            "id": 106535972,
                            "sort_order": 1,
                            "x_offset": 0
                            }
                        }
                    ]
                }""");

        val flow = Flow.of(JsonNode.class).map(parseJsonNodes());
        val system = ActorSystem.create();
        Source.single(jsonNodeExample).via(flow).to(Sink.ignore()).run(system);
    }
}
