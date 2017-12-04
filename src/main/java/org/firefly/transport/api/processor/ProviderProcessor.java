package org.firefly.transport.api.processor;

import org.firefly.model.rpc.request.JRequestBytes;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.model.transport.configuration.Status;

/**
 * Provider's processor.
 */
public interface ProviderProcessor {

    /**
     * 处理正常请求
     */
    void handleRequest(JChannel channel, JRequestBytes request) throws Exception;

    /**
     * 处理异常
     */
    void handleException(JChannel channel, JRequestBytes request, Status status, Throwable cause);
}
