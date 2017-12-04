package org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster;

import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;

public interface ClusterInvoker {

    /**
     * 集群容错策略
     */
    enum Strategy {
        FAIL_FAST,  // 快速失败
        FAIL_OVER,  // 失败重试
        FAIL_SAFE,  // 失败安全
        // FAIL_BACK,  没想到合适场景, 暂不支持
        // FORKING,    消耗资源太多, 暂不支持
        ;

        public static Strategy parse(String name) {
            for (Strategy s : values()) {
                if (s.name().equalsIgnoreCase(name)) {
                    return s;
                }
            }
            return null;
        }

        public static Strategy getDefault() {
            return FAIL_FAST;
        }
    }

    Strategy strategy();

    <T> InvokeFuture<T> invoke(String methodName, Object[] args, Class<T> returnType) throws Exception;
}
