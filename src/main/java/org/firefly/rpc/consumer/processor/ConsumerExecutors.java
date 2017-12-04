package org.firefly.rpc.consumer.processor;

import org.firefly.common.util.spi.JServiceLoader;
import org.firefly.rpc.executor.ConsumerExecutorFactory;
import org.firefly.rpc.executor.ExecutorFactory;
import java.util.concurrent.Executor;

public class ConsumerExecutors {

    private static final Executor executor;

    static {
        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.load(ConsumerExecutorFactory.class).first();
        executor = factory.newExecutor(ExecutorFactory.Target.CONSUMER, "firefly-consumer-processor");
    }

    public static Executor executor() {
        return executor;
    }

    public static void execute(Runnable command) {
        executor.execute(command);
    }
}
