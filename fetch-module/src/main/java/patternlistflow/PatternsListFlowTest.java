package patternlistflow;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import lombok.val;
import pageinfosource.PageInfoSource;
import patternidflow.ResponseToJsonFlow;

import java.util.Arrays;



class PatternsListFlowTest {
    public static void main(String[] args) {
        val patternListFlow = new PatternListFlow("/patterns.json");
        val responseToJsonFlow = new ResponseToJsonFlow();
        int[] arr = {600, 601, 602};
        Iterable<Integer> ids = () -> Arrays.stream(arr)
                .boxed()
                .iterator();
        val system = ActorSystem.create();
        Source.single(ids).via(patternListFlow.create()).via(responseToJsonFlow.create()).to(Sink.ignore()).run(system);
    }
}
