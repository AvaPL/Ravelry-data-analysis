package patternidflow;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;

import java.io.IOException;


public class ResponseToJsonFlow {

    public Flow<Response, JsonNode, NotUsed> create() {
        return Flow.<Response>create().map(this::convert);
    }

    private JsonNode convert(Response response) throws IOException {
        final String responseJsonString = response.body().string();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseJsonString);
    }
}
