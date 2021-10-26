package util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParseJsonFunctionsTest {

    private final ParseJsonFunctions parseJsonFunctions = new ParseJsonFunctions();
    private final ObjectMapper mapper = new ObjectMapper();

    @SneakyThrows
    @Test
    void parseJsonNodeToJsonNodesTest() {
        // example based on result of getting patterns from Ravelry by ids
        JsonNode given = mapper.readTree("""
                {
                    "patterns": {
                        "1" : {
                            "id": 1,
                            "name": "Shawlography: Westknits MKAL 2021",
                            "first_photo": {
                                "id": 106535972,
                                "sort_order": 1
                            }
                        },
                        "2" : {
                            "id": 2,
                            "name": "Slipstravaganza",
                            "first_photo": {
                                "id": 106535972,
                                "sort_order": 1
                            }
                        }
                    }
                }
                """);

        List<JsonNode> expected = new ArrayList<>();
        expected.add(mapper.readTree("""
                {
                    "id": 1,
                    "name": "Shawlography: Westknits MKAL 2021",
                    "first_photo": {
                        "id": 106535972,
                        "sort_order": 1
                    }
                }
                """));
        expected.add(mapper.readTree("""
                {
                    "id": 2,
                    "name": "Slipstravaganza",
                    "first_photo": {
                        "id": 106535972,
                        "sort_order": 1
                    }
                }
                """));

        assertEquals(expected, parseJsonFunctions.parseJsonNodeToJsonNodes("patterns").apply(given));
    }

    @SneakyThrows
    @Test
    void parseJsonNodeToIdsTest() {
        // example based on result of getting patterns from Ravelry through search
        JsonNode given = mapper.readTree("""
                {
                    "patterns": [
                        {
                            "id": 1,
                            "name": "Shawlography: Westknits MKAL 2021",
                            "first_photo": {
                                "id": 106535972,
                                "sort_order": 1
                            }
                        },
                        {
                            "id": 2,
                            "name": "Slipstravaganza",
                            "first_photo": {
                                "id": 106535972,
                                "sort_order": 1
                            }
                        }
                    ]
                }""");

        List<Integer> expected = List.of(1, 2);
        assertEquals(expected, parseJsonFunctions.parseJsonNodeToIds("patterns").apply(given));
    }

    // TODO: move or delete
//    @SneakyThrows
//    @Test
//    void parseJsonNodeToYarnIdsTest() {
//        // example based on a single pattern gotten from Ravelry by ids
//        JsonNode given = mapper.readTree("""
//                {
//                    "comments_count": 37,
//                    "created_at": "2021/09/01 10:00:26 -0400",
//                    "packs": [
//                        {
//                            "id": 101189144,
//                            "yarn_id": 27830,
//                            "yarn_name": "Isager Yarn Isager Alpaca 2"
//                        },
//                        {
//                            "id": 101189138,
//                            "yarn_id": 46174,
//                            "yarn_name": "Holst Garn Supersoft 100% Uld"
//                        }
//                    ]
//                }""");
//
//        Set<Integer> expected = Set.of(27830, 46174);
//        assertEquals(expected, parseJsonFunctions.parseJsonNodesToYarnIds().apply(Collections.singletonList(given)));
//    }
}