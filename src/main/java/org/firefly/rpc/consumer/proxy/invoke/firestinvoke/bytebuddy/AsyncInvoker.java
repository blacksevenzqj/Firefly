package org.firefly.rpc.consumer.proxy.invoke.firestinvoke.bytebuddy;

import org.firefly.common.util.Reflects;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.future.InvokeFutureContext;
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
 * Asynchronous call, {@link #invoke(Method, Object[])}
 * returns a default value of the corresponding method.
 * 异步调用.
 */
public class AsyncInvoker extends ClusterStrategyBridging {

    public AsyncInvoker(FClient client,
                        Dispatcher dispatcher,
                        ClusterStrategyConfig defaultStrategy,
                        List<MethodSpecialConfig> methodSpecialConfigs) {

        super(client, dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    @RuntimeType
    public Object invoke(@Origin Method method, @AllArguments @RuntimeType Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?> returnType = method.getReturnType();
        ClusterInvoker invoker = getClusterInvoker(methodName);
        InvokeFuture<?> future = invoker.invoke(methodName, args, returnType);
        InvokeFutureContext.set(future);
        return Reflects.getTypeDefaultValue(returnType);
    }
}
