package util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParseJsonFunctionsTest {

    @SneakyThrows
    @Test
    void parseJsonNodeToJsonNodesTest() {
        val mapper = new ObjectMapper();
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

        assertEquals(expected, ParseJsonFunctions.parseJsonNodeToJsonNodes("patterns").apply(given));
    }

    @SneakyThrows
    @Test
    void parseJsonNodeToIdsTest() {
        // example based on result of getting patterns from Ravelry through search
        val mapper = new ObjectMapper();
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
        assertEquals(expected, ParseJsonFunctions.parseJsonNodeToIds("patterns").apply(given));
    }

}