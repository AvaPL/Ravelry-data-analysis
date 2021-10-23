package pageinfosource;

import akka.actor.ActorSystem;
import akka.japi.function.Procedure;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import lombok.val;
import pojo.PageInfo;

public class PageInfoSourceTest {
    // TODO: Class for testing PageInfoSource, should be deleted later
    public static void main(String[] args) {
        val system = ActorSystem.create();
        val source = new PageInfoSource(109, 10).getSource();
        val flow = Flow.of(PageInfo.class).map(Object::toString);
        val sink = Sink.foreach((Procedure<String>) System.out::println);
        val graph = source.via(flow).toMat(sink, Keep.right());
        val completionStage = graph.run(system);
        completionStage.whenComplete((Object d, Object t) -> system.terminate());
    }
}
