package raverlyapicallflow;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import lombok.val;
import pageinfosource.PageInfoSource;
import pojo.PageInfo;

import java.util.HashMap;
import java.util.Map;


class RavelryApiCallFlowTest {

    private static Map<String, String> pageInfoToParams(PageInfo pageInfo) {
        Map<String, String> params = new HashMap<>();
        params.put("page", pageInfo.getPageNumber().toString());
        params.put("page_size", pageInfo.getPageLimit().toString());
        return params;
    }

    public static void main(String[] args) {
        PageInfoSource source = new PageInfoSource(100, 10);
        val getIDsFlow = new RavelryApiCallFlow<>("/patterns/search.json", RavelryApiCallFlowTest::pageInfoToParams);
        val system = ActorSystem.create();
        source.create().via(getIDsFlow.create()).to(Sink.ignore()).run(system);
    }
}
