package org.firefly.common.concurrent.thread.executorfactory;

import org.firefly.common.util.SystemPropertyUtil;
import org.firefly.common.util.constant.JConstants;
import org.firefly.rpc.executor.ExecutorFactory;

public abstract class AbstractExecutorFactory implements ExecutorFactory {

    protected int coreWorkers(Target target) {
        switch (target) {
            case CONSUMER:
                return SystemPropertyUtil.getInt(CONSUMER_EXECUTOR_CORE_WORKERS, JConstants.AVAILABLE_PROCESSORS << 1);
            case PROVIDER:
                return SystemPropertyUtil.getInt(PROVIDER_EXECUTOR_CORE_WORKERS, JConstants.AVAILABLE_PROCESSORS << 4);
            default:
                throw new IllegalArgumentException(String.valueOf(target));
        }
    }

    protected int maxWorkers(Target target) {
        switch (target) {
            case CONSUMER:
                return SystemPropertyUtil.getInt(CONSUMER_EXECUTOR_MAX_WORKERS, JConstants.AVAILABLE_PROCESSORS << 3);
            case PROVIDER:
                return SystemPropertyUtil.getInt(PROVIDER_EXECUTOR_MAX_WORKERS, JConstants.AVAILABLE_PROCESSORS << 7);
            default:
                throw new IllegalArgumentException(String.valueOf(target));
        }
    }

    protected int queueCapacity(Target target) {
        switch (target) {
            case CONSUMER:
                return SystemPropertyUtil.getInt(CONSUMER_EXECUTOR_QUEUE_CAPACITY, 32768);
            case PROVIDER:
                return SystemPropertyUtil.getInt(PROVIDER_EXECUTOR_QUEUE_CAPACITY, 32768);
            default:
                throw new IllegalArgumentException(String.valueOf(target));
        }
    }
}
