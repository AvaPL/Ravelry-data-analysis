package raverlyapicallflow;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;
import raverlyapi.RavelryApi;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@AllArgsConstructor
public class RavelryApiCallFlow<T> {

    private String url;
    private Function<T, Map<String, String>> paramToMap;

    public Flow<T, JsonNode, NotUsed> create() {
        return Flow.<T>create().mapAsync(1, this::getDataFromApiSafe);
    }

    private CompletableFuture<JsonNode> getDataFromApiSafe(T flowParameter) {
        val retryPolicy = getRetryPolicy();
        val fallback = getFallback();
        return Failsafe.with(fallback, retryPolicy).getStageAsync(() -> getDataFromApi(flowParameter));
    }


    private RetryPolicy<JsonNode> getRetryPolicy() {
        return new RetryPolicy<JsonNode>()
                .withBackoff(3, 30, ChronoUnit.SECONDS)
                .withMaxRetries(5)
                .onFailedAttempt(e -> e.getLastFailure().printStackTrace())
                .onRetry(e -> System.out.println("Retrying Ravelry API call"))
                .onRetriesExceeded(e -> System.err.println("Exceeded retries for Ravelry API calls"));
    }

    private Fallback<JsonNode> getFallback() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.createObjectNode();
        return Fallback.of(jsonNode)
                .onSuccess(e -> System.out.println("Passing down empty json node"));
    }

    @SneakyThrows
    private JsonNode responseToJson(Response response) {
        String responseJsonString;
        responseJsonString = response.body().string();
        System.out.println(responseJsonString);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(responseJsonString);
        } catch (JsonProcessingException e) {
            return mapper.createObjectNode();
        }
    }

    private CompletableFuture<JsonNode> getDataFromApi(T flowParameter) {
        return RavelryApi.getRavelryData(url, paramToMap.apply(flowParameter)).thenApply(this::responseToJson);
    }

}
