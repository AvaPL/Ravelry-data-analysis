import akka.Done;
import akka.stream.javadsl.RunnableGraph;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import graphs.PatternsAndYarnsGraph;
import graphs.RavelryGraphsRunner;
import lombok.val;
import okhttp3.OkHttpClient;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionStage;

public class Main extends RavelryGraphsRunner {

    public static void main(String[] args) {
        val main = new Main();
        main.run();
    }

    @Override
    protected Collection<RunnableGraph<CompletionStage<Done>>> getGraphs(
            OkHttpClient apiClient,
            ElasticsearchAsyncClient esClient
    ) {
        val patternsGraph = PatternsAndYarnsGraph.create(apiClient, esClient);
        return Arrays.asList(patternsGraph);
    }
}
