package esflow;


import okhttp3.ResponseBody;
import raverlyapi.RavelryApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class GetIDsFlow {

    //TODO Needs refactoring
    public static ResponseBody getIDs(String url, Integer pageNumber, Integer pageLimit) {
        Map<String, String> params = new HashMap<>();
        params.put("page", pageNumber.toString());
        params.put("page_size", pageLimit.toString());
        ResponseBody responseBody = null;
        try {
           responseBody = RavelryApi.getRavelryData(url, params);
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return responseBody;
    }
}
