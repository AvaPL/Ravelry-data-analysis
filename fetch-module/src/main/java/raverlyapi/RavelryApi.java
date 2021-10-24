package raverlyapi;


import okhttp3.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;



public class RavelryApi {
    private static final OkHttpClient client = new OkHttpClient();
    private static final String username = System.getenv("USERNAME");
    private static final String password = System.getenv("PASSWORD");
    private static final String BASE_URL = "https://api.ravelry.com";


    public static CompletableFuture<Response> getRavelryData(String url, Map<String, String> parameters) {
        HttpUrl.Builder httpBuilder = HttpUrl.parse(BASE_URL + url).newBuilder();
        String credentials = Credentials.basic(username, password);

        if (!parameters.isEmpty()) {
            for(Map.Entry<String, String> param : parameters.entrySet()) {
                httpBuilder.addQueryParameter(param.getKey(),param.getValue());
            }
        }

        Request request = new Request.Builder()
                .addHeader("Authorization", credentials)
                .url(httpBuilder.build())
                .build();

        Call call = client.newCall(request);
        CallbackFuture future = new CallbackFuture();
        call.enqueue(future);
        return future;
    }

    public static Future<Response> getRavelryData(String url) {
        return getRavelryData(url, new HashMap<>());
    }

}

