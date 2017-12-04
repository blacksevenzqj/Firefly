package org.firefly.rpc.executor;

import java.util.concurrent.Executor;

public interface ExecutorFactory extends ConsumerExecutorFactory, ProviderExecutorFactory {

    Executor newExecutor(Target target, String name);

    enum Target {
        CONSUMER,
        PROVIDER
    }

    String CONSUMER_EXECUTOR_CORE_WORKERS           = "firefly.executor.factory.consumer.core.workers";
    String PROVIDER_EXECUTOR_CORE_WORKERS           = "firefly.executor.factory.provider.core.workers";
    String CONSUMER_EXECUTOR_MAX_WORKERS            = "firefly.executor.factory.consumer.max.workers";
    String PROVIDER_EXECUTOR_MAX_WORKERS            = "firefly.executor.factory.provider.max.workers";
    String CONSUMER_EXECUTOR_QUEUE_TYPE             = "firefly.executor.factory.consumer.queue.type";
    String PROVIDER_EXECUTOR_QUEUE_TYPE             = "firefly.executor.factory.provider.queue.type";
    String CONSUMER_EXECUTOR_QUEUE_CAPACITY         = "firefly.executor.factory.consumer.queue.capacity";
    String PROVIDER_EXECUTOR_QUEUE_CAPACITY         = "firefly.executor.factory.provider.queue.capacity";
    String CONSUMER_DISRUPTOR_WAIT_STRATEGY_TYPE    = "firefly.executor.factory.consumer.disruptor.wait.strategy.type";
    String PROVIDER_DISRUPTOR_WAIT_STRATEGY_TYPE    = "firefly.executor.factory.provider.disruptor.wait.strategy.type";
    String CONSUMER_THREAD_POOL_REJECTED_HANDLER    = "firefly.executor.factory.consumer.thread.pool.rejected.handler";
    String PROVIDER_THREAD_POOL_REJECTED_HANDLER    = "firefly.executor.factory.provider.thread.pool.rejected.handler";
}
