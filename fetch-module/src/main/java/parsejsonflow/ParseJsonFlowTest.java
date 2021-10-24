package parsejsonflow;

import akka.japi.function.Function;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import java.util.*;
import java.util.stream.Collectors;

public class ParseJsonFlowTest {

    // Function for parsing a single json node to an iterable of json nodes. To be used later in main.
    // TODO: delete static and sout
    public static Function<JsonNode, Iterable<JsonNode>> parseJsonNodeToJsonNodes(String nodesListName) {
        return jsonNode -> {
            Iterator<JsonNode> jsonNodesIterator = jsonNode.get(nodesListName).elements();
            List<JsonNode> jsonNodeList = Lists.newArrayList(jsonNodesIterator);
            System.out.println(jsonNodeList);
            return jsonNodeList;
        };
    }

    // Function for parsing a patterns json node to an iterable of pattern ids.
    // TODO: delete static and sout
    public static Function<JsonNode, Iterable<Integer>> parseJsonNodeToIds(String nodesListName) {
        return jsonNode -> {
            List<JsonNode> jsonNodeList = (List<JsonNode>) parseJsonNodeToJsonNodes(nodesListName).apply(jsonNode);
            List<Integer> ids = jsonNodeList.stream().map(jsonNode1 -> jsonNode1.get("id").asInt()).collect(Collectors.toList());
            System.out.println(ids);
            return ids;
        };
    }

    // Function for parsing a patterns json node to an iterable of pattern ids.
    // TODO: delete static and sout
    public static Function<Iterable<JsonNode>, Iterable<Integer>> parseJsonNodesToYarnIds() {
        return jsonNodes -> {
            Set<Integer> yarnIds = new HashSet<>();
            for (JsonNode jsonNode : jsonNodes) {
                List<JsonNode> yarns = (List<JsonNode>) parseJsonNodeToJsonNodes("packs").apply(jsonNode);
                yarns.forEach(jsonNode1 -> yarnIds.add(jsonNode1.get("yarn_id").asInt()));
            }
            System.out.println(yarnIds);
            return yarnIds;
        };
    }

    // Main for testing parsing functions.
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode searchExample = mapper.readTree("""
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

        JsonNode patternsByIdsExample = mapper.readTree("""
                {
                    "patterns": {
                        "123" : {
                            "free": false,
                            "id": 1,
                            "name": "Shawlography: Westknits MKAL 2021",
                            "first_photo": {
                            "id": 106535972,
                            "sort_order": 1,
                            "x_offset": 0
                            }
                        },
                        "321" : {
                            "free": false,
                            "id": 2,
                            "name": "Slipstravaganza",
                            "first_photo": {
                            "id": 106535972,
                            "sort_order": 1,
                            "x_offset": 0
                            }
                        }
                    }
                }""");

        JsonNode yarnIdsExample = mapper.readTree("""
                {
                    "comments_count": 37,
                    "created_at": "2021/09/01 10:00:26 -0400",
                    "packs": [
                        {
                        "id": 101189144,
                        "yarn_id": 27830,
                        "yarn_name": "Isager Yarn Isager Alpaca 2"
                        },
                        {
                        "id": 101189138,
                        "yarn_id": 46174,
                        "yarn_name": "Holst Garn Supersoft 100% Uld"
                        }
                    ]
                }""");

        parseJsonNodeToIds("patterns").apply(searchExample);
        parseJsonNodeToJsonNodes("patterns").apply(patternsByIdsExample);
        parseJsonNodesToYarnIds().apply(Collections.singletonList(yarnIdsExample));
    }
}
