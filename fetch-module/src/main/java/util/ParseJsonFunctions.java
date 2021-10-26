package util;

import akka.japi.function.Function;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ParseJsonFunctions {

    // Function for parsing a single json node to an iterable of json nodes. To be used later in main.
    public static Function<JsonNode, Collection<JsonNode>> parseJsonNodeToJsonNodes(String nodesListName) {
        return jsonNode -> {
            Iterator<JsonNode> jsonNodesIterator = jsonNode.get(nodesListName).elements();
            return Lists.newArrayList(jsonNodesIterator);
        };
    }

    // Function for parsing a patterns json node to an iterable of pattern ids.
    public static Function<JsonNode, Collection<Integer>> parseJsonNodeToIds(String nodesListName) {
        return jsonNode -> {
            List<JsonNode> jsonNodeList = (List<JsonNode>) parseJsonNodeToJsonNodes(nodesListName).apply(jsonNode);
            return jsonNodeList.stream().map(jsonNode1 -> jsonNode1.get("id").asInt()).collect(Collectors.toList());
        };
    }
}
