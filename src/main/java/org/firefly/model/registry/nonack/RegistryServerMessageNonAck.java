package org.firefly.model.registry.nonack;

import io.netty.channel.Channel;
import org.firefly.common.util.SystemClock;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.transport.netty.acceptor.DefaultRegistryServerNettyTcpAcceptor;

/**
 * 没收到ACK, 需要重发消息
 * 1、“注册服务” ：发送 回复信息给 “服务提供者或消费者” 时，同时发送 确认消息给 “服务提供者或消费者”。
 * 2、要求“服务提供者或消费者”收到确认消息后，发送回复ACK消息给“注册服务”并删除ConcurrentMap<String, MessageNonAck>中相应的 RegistryServerMessageNonAck
 * 3、RegistryServerMessageNonAck 由 “注册服务” 持有
 */
public class RegistryServerMessageNonAck {
    public final String id;

    public final RegisterMeta.ServiceMeta serviceMeta;
    public final PublishSubscriptionMessage msg;
    public final Channel channel;
    public final long version;
    public final long timestamp = SystemClock.millisClock().now();

    public RegistryServerMessageNonAck(RegisterMeta.ServiceMeta serviceMeta, PublishSubscriptionMessage msg, Channel channel) {
        this.serviceMeta = serviceMeta;
        this.msg = msg;
        this.channel = channel;
        this.version = msg.version();

        id = DefaultRegistryServerNettyTcpAcceptor.key(msg.sequence(), channel);
    }
}
