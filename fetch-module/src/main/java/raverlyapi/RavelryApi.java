package raverlyapi;

import okhttp3.*;

import java.io.IOException;
import java.util.Map;

public class RavelryApi {
    private static final OkHttpClient client = new OkHttpClient();
    private static final String username = System.getenv("USERNAME");
    private static final String password = System.getenv("PASSWORD");
    private static final String BASE_URL = "https://api.ravelry.com/";


    public static ResponseBody getRavelryData(String url, Map<String, String> parameters) throws IOException {
        HttpUrl.Builder httpBuilder = HttpUrl.parse(BASE_URL + url).newBuilder();
        String credentials = Credentials.basic(username, password);

        if (parameters != null) {
            for(Map.Entry<String, String> param : parameters.entrySet()) {
                httpBuilder.addQueryParameter(param.getKey(),param.getValue());
            }
        }

        Request request = new Request.Builder()
                .addHeader("Authorization", credentials)
                .url(httpBuilder.build())
                .build();

        Call call = client.newCall(request);
        return call.execute().body();
    }

    public static ResponseBody getRavelryData(String url) throws IOException {
        return getRavelryData(url, null);
    }

}

