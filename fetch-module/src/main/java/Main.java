import akka.Done;
import akka.stream.javadsl.*;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import graphs.PatternsGraph;
import graphs.RavelryGraphsRunner;
import lombok.val;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionStage;

public class Main extends RavelryGraphsRunner {

    public static void main(String[] args) {
        val main = new Main();
        main.run();
    }

    @Override
    protected Collection<RunnableGraph<CompletionStage<Done>>> getGraphs(ElasticsearchAsyncClient esClient) {
        val patternsGraph = PatternsGraph.create(esClient);
        return Arrays.asList(patternsGraph);
    }
}
