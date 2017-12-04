package org.firefly.rpc.consumer.proxy.hook;

import org.firefly.model.rpc.request.JRequest;
import org.firefly.model.rpc.response.JResponse;
import org.firefly.model.transport.channel.interfice.JChannel;

/**
 * 客户端的钩子函数.
 * 在请求发送时触发 {@link #before(JRequest, JChannel)} 方法;
 * 在响应回来时触发 {@link #after(JResponse, JChannel)} 方法.
 */
public interface ConsumerHook {

    ConsumerHook[] EMPTY_HOOKS = new ConsumerHook[0];

    /**
     * Triggered when the request data sent to the network.
     */
    void before(JRequest request, JChannel channel);

    /**
     * Triggered when the server returns the result.
     */
    void after(JResponse response, JChannel channel);
}
