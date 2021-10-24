package patternidflow;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import lombok.AllArgsConstructor;
import okhttp3.Response;
import pojo.PageInfo;
import raverlyapi.RavelryApi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class GetIDsFlow {

    private String url;

    public Flow<PageInfo, Response, NotUsed> create() {
        return Flow.<PageInfo>create().mapAsync(1, this::getIDs);
    }

    private CompletableFuture<Response> getIDs(PageInfo pageInfo) {
        Map<String, String> params = new HashMap<>();
        params.put("page", pageInfo.getPageNumber().toString());
        params.put("page_size", pageInfo.getPageLimit().toString());
        return RavelryApi.getRavelryData(url, params);
    }
}
