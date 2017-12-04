package org.firefly.rpc.consumer.proxy.dispatch;

import org.firefly.model.rpc.consumer.cluster.MethodSpecialConfig;
import org.firefly.model.rpc.metadata.ServiceMetadata;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;
import org.firefly.rpc.consumer.proxy.hook.ConsumerHook;

import java.util.List;

public interface Dispatcher {

    <T> InvokeFuture<T> dispatch(FClient client, String methodName, Object[] args, Class<T> returnType);

    ServiceMetadata metadata();

    Dispatcher hooks(List<ConsumerHook> hooks);

    Dispatcher timeoutMillis(long timeoutMillis);

    Dispatcher methodSpecialConfigs(List<MethodSpecialConfig> methodSpecialConfigs);
}
