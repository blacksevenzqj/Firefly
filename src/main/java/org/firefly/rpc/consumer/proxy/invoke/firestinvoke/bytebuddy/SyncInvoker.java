package org.firefly.rpc.consumer.proxy.invoke.firestinvoke.bytebuddy;

import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.invoke.firestinvoke.ClusterStrategyBridging;
import org.firefly.model.rpc.consumer.cluster.ClusterStrategyConfig;
import org.firefly.model.rpc.consumer.cluster.MethodSpecialConfig;
import org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster.ClusterInvoker;
import org.firefly.rpc.consumer.proxy.dispatch.Dispatcher;
import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Synchronous call.
 * 同步调用.
 */
public class SyncInvoker extends ClusterStrategyBridging {

    public SyncInvoker(FClient client,
                       Dispatcher dispatcher,
                       ClusterStrategyConfig defaultStrategy,
                       List<MethodSpecialConfig> methodSpecialConfigs) {

        super(client, dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    @RuntimeType
    public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
        String methodName = method.getName();
        ClusterInvoker invoker = getClusterInvoker(methodName);
        InvokeFuture<?> future = invoker.invoke(methodName, args, method.getReturnType());
        return future.getResult();
    }
}
