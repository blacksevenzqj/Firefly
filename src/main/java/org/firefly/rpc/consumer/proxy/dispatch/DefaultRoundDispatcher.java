package org.firefly.rpc.consumer.proxy.dispatch;

import org.firefly.model.rpc.metadata.ServiceMetadata;
import org.firefly.model.rpc.request.JRequest;
import org.firefly.model.rpc.request.MessageWrapper;
import org.firefly.model.rpc.type.DispatchType;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.balance.interfice.LoadBalancer;
import org.firefly.rpc.consumer.proxy.future.DefaultInvokeFuture;
import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerType;

/**
 * 单播方式派发消息.
 */
public class DefaultRoundDispatcher extends AbstractDispatcher {

    public DefaultRoundDispatcher(
            LoadBalancer loadBalancer, ServiceMetadata metadata, SerializerType serializerType) {
        super(loadBalancer, metadata, serializerType);
    }

    // 是从动态代理传进来的参数：
    @Override
    public <T> InvokeFuture<T> dispatch(FClient client, String methodName, Object[] args, Class<T> returnType) {
        // stack copy
        // SerializerType 默认为：PROTO_STUFF ((byte) 0x01) ---> ProtoStuffSerializer
        final Serializer _serializer = serializer();

        MessageWrapper message = new MessageWrapper(metadata());
        message.setAppName(client.appName()); // 默认：UNKNOWN
        message.setMethodName(methodName);
        // 不需要方法参数类型, 服务端会根据args具体类型按照JLS规则动态dispatch
        message.setArgs(args);

        // 通过软负载均衡选择一个channel
        JChannel channel = select(client);

        doTracing(message, channel);

        byte s_code = _serializer.code();  // ProtoStuffSerializer.code() = (byte) 0x01
        // 在业务线程中序列化, 减轻IO线程负担
        byte[] bytes = _serializer.writeObject(message);

        JRequest request = new JRequest();
        request.message(message);
        request.bytes(s_code, bytes);

        long timeoutMillis = getMethodSpecialTimeoutMillis(methodName);
        DefaultInvokeFuture<T> future = DefaultInvokeFuture
                .with(request.invokeId(), channel, returnType, timeoutMillis, DispatchType.ROUND)
                .hooks(hooks());

        return write(channel, request, future, DispatchType.ROUND);
    }
}
