package org.firefly.rpc.consumer.proxy.invoke.firestinvoke.generic;

import org.firefly.model.rpc.consumer.cluster.ClusterStrategyConfig;
import org.firefly.model.rpc.consumer.cluster.MethodSpecialConfig;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster.ClusterInvoker;
import org.firefly.rpc.consumer.proxy.dispatch.Dispatcher;
import org.firefly.rpc.consumer.proxy.future.InvokeFutureContext;
import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;
import org.firefly.rpc.consumer.proxy.invoke.firestinvoke.ClusterStrategyBridging;

import java.util.List;

/**
 * 异步泛化调用.
 */
public class AsyncGenericInvoker extends ClusterStrategyBridging implements GenericInvoker {

    public AsyncGenericInvoker(FClient client,
                               Dispatcher dispatcher,
                               ClusterStrategyConfig defaultStrategy,
                               List<MethodSpecialConfig> methodSpecialConfigs) {

        super(client, dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    @Override
    public Object invoke(String methodName, Object... args) throws Throwable {
        ClusterInvoker invoker = getClusterInvoker(methodName);
        InvokeFuture<?> future = invoker.invoke(methodName, args, Object.class);
        InvokeFutureContext.set(future);
        return null;
    }
}
