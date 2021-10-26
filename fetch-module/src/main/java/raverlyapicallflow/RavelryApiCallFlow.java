package raverlyapicallflow;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.Fallback;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import ravelryapi.RavelryApi;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@AllArgsConstructor
@Log4j2
public class RavelryApiCallFlow<T> {

    private OkHttpClient client;
    private String url;
    private Function<T, Map<String, String>> extractUrlParams;

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
                .onFailedAttempt(e -> log.error("Failed to fetch data from API [url: " + url + "]", e.getLastFailure()))
                .onRetry(e -> log.info("Retrying Ravelry API call [url: " + url + "]"))
                .onRetriesExceeded(e -> log.error("Exceeded retries for Ravelry API calls [url: " + url + "]"));
    }

    private Fallback<JsonNode> getFallback() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.createObjectNode();
        return Fallback.of(jsonNode)
                .onSuccess(e -> log.debug("Passing json node downstream"));
    }

    @SneakyThrows
    private JsonNode responseToJson(Response response) {
        String responseJsonString = Objects.requireNonNull(response.body()).string();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseJsonString);
    }

    private CompletableFuture<JsonNode> getDataFromApi(T flowParameter) {
        return RavelryApi.getRavelryData(client, url, extractUrlParams.apply(flowParameter))
                .thenApply(this::responseToJson);
    }
}
