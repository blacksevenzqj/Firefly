package org.firefly.rpc.consumer.processor;

import org.firefly.model.rpc.response.JResponse;
import org.firefly.model.rpc.response.JResponseBytes;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.rpc.consumer.processor.task.ConsumerMessageTask;
import org.firefly.transport.api.processor.ConsumerProcessor;

import java.util.concurrent.Executor;

public class DefaultConsumerProcessor implements ConsumerProcessor {

    private final Executor executor;

    public DefaultConsumerProcessor() {
        this(ConsumerExecutors.executor());
    }

    public DefaultConsumerProcessor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void handleResponse(JChannel channel, JResponseBytes responseBytes) throws Exception {
        ConsumerMessageTask task = new ConsumerMessageTask(channel, new JResponse(responseBytes));
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }
}
