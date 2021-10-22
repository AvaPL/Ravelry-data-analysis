import okhttp3.ResponseBody;
import raverlyapi.RavelryApi;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            ResponseBody res = RavelryApi.getRavelryData("patterns/patterns.json");
            System.out.print(res.string());
        }
        catch (IOException ex) {
            System.out.println("error");
        }
    }
}
