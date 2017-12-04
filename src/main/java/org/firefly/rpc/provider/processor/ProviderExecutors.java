package org.firefly.rpc.provider.processor;

import org.firefly.common.util.spi.JServiceLoader;
import org.firefly.rpc.executor.ExecutorFactory;
import org.firefly.rpc.executor.ProviderExecutorFactory;

import java.util.concurrent.Executor;

public class ProviderExecutors {

    private static final Executor executor;

    static {
        ExecutorFactory factory = (ExecutorFactory) JServiceLoader.load(ProviderExecutorFactory.class).first();
        executor = factory.newExecutor(ExecutorFactory.Target.PROVIDER, "firefly-provider-processor");
    }

    public static Executor executor() {
        return executor;
    }

    public static void execute(Runnable command) {
        executor.execute(command);
    }
}
