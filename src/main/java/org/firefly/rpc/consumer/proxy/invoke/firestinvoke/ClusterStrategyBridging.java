package org.firefly.rpc.consumer.proxy.invoke.firestinvoke;

import org.firefly.common.util.internal.Maps;
import org.firefly.model.rpc.consumer.cluster.ClusterStrategyConfig;
import org.firefly.model.rpc.consumer.cluster.MethodSpecialConfig;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster.ClusterInvoker;
import org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster.FailFastClusterInvoker;
import org.firefly.rpc.consumer.proxy.dispatch.Dispatcher;
import java.util.List;
import java.util.Map;

public abstract class ClusterStrategyBridging {

    private final ClusterInvoker defaultClusterInvoker;
    private final Map<String, ClusterInvoker> methodSpecialClusterInvokerMapping;

    public ClusterStrategyBridging(FClient client,
                                   Dispatcher dispatcher,
                                   ClusterStrategyConfig defaultStrategy,
                                   List<MethodSpecialConfig> methodSpecialConfigs) {

        this.defaultClusterInvoker = createClusterInvoker(client, dispatcher, defaultStrategy);
        this.methodSpecialClusterInvokerMapping = Maps.newHashMap();
        for (MethodSpecialConfig config : methodSpecialConfigs) {
            ClusterStrategyConfig strategy = config.getStrategy();
            if (strategy != null) {
                methodSpecialClusterInvokerMapping.put(
                        config.getMethodName(),
                        createClusterInvoker(client, dispatcher, strategy)
                );
            }
        }
    }

    public ClusterInvoker getClusterInvoker(String methodName) {
        ClusterInvoker invoker = methodSpecialClusterInvokerMapping.get(methodName);
        return invoker != null ? invoker : defaultClusterInvoker;
    }

    private ClusterInvoker createClusterInvoker(FClient client, Dispatcher dispatcher, ClusterStrategyConfig strategy) {
        ClusterInvoker.Strategy s = strategy.getStrategy();
        switch (s) {
            case FAIL_FAST:
                return new FailFastClusterInvoker(client, dispatcher);
            case FAIL_OVER:
//                return new FailOverClusterInvoker(client, dispatcher, strategy.getFailoverRetries());
            case FAIL_SAFE:
//                return new FailSafeClusterInvoker(client, dispatcher);
            default:
                throw new UnsupportedOperationException("strategy: " + strategy);
        }
    }
}
