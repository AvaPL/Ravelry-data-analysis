import akka.actor.ActorSystem;
import akka.japi.function.Procedure;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import lombok.val;

public class Main {
    public static void main(String[] args) {
        val system = ActorSystem.create();
        val source = Source.range(1, 5);
        val flow = Flow.of(Integer.class).map(Object::toString);
        val sink = Sink.foreach((Procedure<String>) System.out::println);
        val graph = source.via(flow).toMat(sink, Keep.right());
        val completionStage = graph.run(system);
        completionStage.whenComplete((Object d, Object t) -> system.terminate());
    }
}
