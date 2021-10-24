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
import java.util.stream.Collectors;

public class ParseJsonFlowTest {

    // Function for parsing a single json node to an iterable of json nodes. To be used later in main.
    // TODO: delete static and sout
    public static Function<JsonNode, Iterable<JsonNode>> parseJsonNodeToJsonNodes(String nodesName) {
        return jsonNode -> {
            ArrayNode arrayNode = (ArrayNode) jsonNode.get(nodesName);
            List<JsonNode> jsonNodeList = new ArrayList<>();
            arrayNode.forEach(jsonNodeList::add);
            System.out.println(jsonNodeList);
            return jsonNodeList;
        };
    }

    // Function for parsing a patterns json node to an iterable of pattern ids.
    // TODO: delete static and sout
    public static Function<JsonNode, Iterable<Integer>> parseJsonNodeToPatternIds(String nodesName) {
        return jsonNode -> {
            List<JsonNode> jsonNodeList = (List<JsonNode>) parseJsonNodeToJsonNodes(nodesName).apply(jsonNode);
            List<Integer> patternIds = jsonNodeList.stream().map(jsonNode1 -> jsonNode1.get("id").asInt()).collect(Collectors.toList());
            System.out.println(patternIds);
            return patternIds;
        };
    }

    // Main for testing parsing functions.
    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNodeExample = mapper.readTree("""
                {
                    "patterns": [
                        {
                            "free": false,
                            "id": 1,
                            "name": "Shawlography: Westknits MKAL 2021",
                            "first_photo": {
                            "id": 106535972,
                            "sort_order": 1,
                            "x_offset": 0
                            }
                        },
                        {
                            "free": false,
                            "id": 2,
                            "name": "Slipstravaganza",
                            "first_photo": {
                            "id": 106535972,
                            "sort_order": 1,
                            "x_offset": 0
                            }
                        }
                    ]
                }""");

        val flow = Flow.of(JsonNode.class).map(parseJsonNodeToPatternIds("patterns"));
        val system = ActorSystem.create();
        Source.single(jsonNodeExample).via(flow).to(Sink.ignore()).run(system);
    }
}