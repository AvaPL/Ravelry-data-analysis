package graphs;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.javadsl.RunnableGraph;
import co.elastic.clients.base.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import okhttp3.OkHttpClient;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Log4j2
public abstract class RavelryGraphsRunner {

    private static final OkHttpClient apiClient = new OkHttpClient();
    private static final RestClient esRestClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
    private static final ActorSystem actorSystem = ActorSystem.create();

    public void run() {
        val esClient = getEsClient();
        val graphs = getGraphs(apiClient, esClient);
        runGraphs(graphs);
    }

    private ElasticsearchAsyncClient getEsClient() {
        val transport = new RestClientTransport(esRestClient, new JacksonJsonpMapper());
        return new ElasticsearchAsyncClient(transport);
    }

    protected abstract Collection<RunnableGraph<CompletionStage<Done>>> getGraphs(
            OkHttpClient apiClient,
            ElasticsearchAsyncClient esClient
    );

    private static void runGraphs(Collection<RunnableGraph<CompletionStage<Done>>> graphs) {
        val graphsDone = graphs.stream()
                .map(graph -> graph.run(actorSystem).toCompletableFuture())
                .toArray(CompletableFuture<?>[]::new);
        val done = CompletableFuture.allOf(graphsDone);
        done.thenRun(RavelryGraphsRunner::cleanup);
    }

    @SneakyThrows
    private static void cleanup() {
        log.info("Closing API client");
        apiClient.dispatcher().executorService().shutdown();
        log.info("Closing ES connection");
        esRestClient.close();
        log.info("Terminating actor system");
        actorSystem.terminate();
    }
}
