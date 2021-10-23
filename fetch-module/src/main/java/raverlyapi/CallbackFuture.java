package raverlyapi;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

class CallbackFuture extends CompletableFuture<Response> implements Callback {
    public void onResponse(@NotNull Call call, @NotNull Response response) {
        super.complete(response);
    }
    public void onFailure(@NotNull Call call, @NotNull IOException e){
        super.completeExceptionally(e);
    }
}
