package ai.konduit.serving.orchestration;

import io.vertx.core.json.JsonObject;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.spi.cluster.ClusterManager;


public class ClusteredInferenceVerticle extends ai.konduit.serving.verticles.inference.InferenceVerticle {

    protected io.vertx.core.eventbus.EventBus eventBus;
    private String verticleAddress = "Consumer";
    private ClusterManager clusterManager;


    @Override
    public void start(io.vertx.core.Future<Void> startFuture) throws Exception {
        super.start(startFuture);
    }

    @Override
    public void start() throws Exception {
        super.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    @Override
    public void init(io.vertx.core.Vertx vertx, io.vertx.core.Context context) {
        super.init(vertx, context);
        eventBus = vertx.eventBus();
    }

    private void sendMessage() {
        JsonObject jsonMessage = new JsonObject().put("message_from_sender_verticle", "hello consumer");
        eventBus.send("Consumer", jsonMessage, messageAsyncResult -> {
            if(messageAsyncResult.succeeded()) {
                JsonObject jsonReply = (io.vertx.core.json.JsonObject) messageAsyncResult.result().body();
                System.out.println("received reply: " + jsonReply.getValue("reply"));
            }
        });
    }

    private void registerHandler() {
        MessageConsumer<JsonObject> messageConsumer = eventBus.consumer(verticleAddress);
        messageConsumer.handler(message -> {
            JsonObject jsonMessage = message.body();
            System.out.println(jsonMessage.getValue("message_from_sender_verticle"));
            JsonObject jsonReply = new io.vertx.core.json.JsonObject().put("reply", "how interesting!");
            message.reply(jsonReply);
        });
    }

}
