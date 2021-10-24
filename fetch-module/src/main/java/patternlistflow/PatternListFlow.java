package patternlistflow;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import lombok.AllArgsConstructor;
import okhttp3.Response;
import raverlyapi.RavelryApi;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
class PatternListFlow {

    private String url;

    public Flow<Iterable<Integer>, Response, NotUsed> create() {
        return Flow.<Iterable<Integer>>create().mapAsync(1, this::getPatterns);
    }

    private CompletableFuture<Response> getPatterns(Iterable<Integer> patternIDs) {
        Map<String, String> params = new HashMap<>();
        StringJoiner stringJoiner = new StringJoiner(" ");
        for(Integer id: patternIDs) {
            stringJoiner.add(id.toString());
        }
        params.put("ids", stringJoiner.toString());
        return RavelryApi.getRavelryData(url, params);
    }
}
