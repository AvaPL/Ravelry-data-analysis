package patternidflow;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import lombok.val;
import pageinfosource.PageInfoSource;

class GetIDsFlowTest {
    public static void main(String[] args) {
        PageInfoSource source = new PageInfoSource(100, 10);
        val getIDsFlow = new GetIDsFlow("/patterns/search.json");
        val responseToJsonFlow = new ResponseToJsonFlow();
        val system = ActorSystem.create();
        source.getSource().via(getIDsFlow.create()).via(responseToJsonFlow.create()).to(Sink.ignore()).run(system);
    }
}
