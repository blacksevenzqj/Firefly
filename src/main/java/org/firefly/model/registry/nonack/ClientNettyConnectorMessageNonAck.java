package org.firefly.model.registry.nonack;

import org.firefly.common.util.SystemClock;
import org.firefly.model.registry.PublishSubscriptionMessage;

/**
 * 1、“服务提供者或消费者”：发送 服务注册或订阅请求给 “注册服务” 时，同时发送 确认消息给 “注册服务”。
 * 2、要求“注册服务”收到确认消息后，发送回复ACK消息给“服务提供者或消费者”并删除ConcurrentMap<String, MessageNonAck>中相应的 ClientNettyConnectorMessageNonAck
 * 3、ClientNettyConnectorMessageNonAck 由 “服务提供者或消费者” 持有
 */
public class ClientNettyConnectorMessageNonAck {

    public final long id;

    public final PublishSubscriptionMessage msg;
    public final long timestamp = SystemClock.millisClock().now();

    public ClientNettyConnectorMessageNonAck(PublishSubscriptionMessage msg) {
        this.msg = msg;
        id = msg.sequence();
    }
}
