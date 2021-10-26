package graphs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatternsAndYarnsGraphTest {

    @SneakyThrows
    @Test
    void parseJsonNodeToYarnIdsTest() {
        // example based on a single pattern gotten from Ravelry by ids
        val mapper = new ObjectMapper();
        JsonNode given = mapper.readTree("""
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

        Set<Integer> expected = Set.of(27830, 46174);
        assertEquals(expected, PatternsAndYarnsGraph.parseJsonNodesToYarnIds().apply(Collections.singletonList(given)));
    }
}