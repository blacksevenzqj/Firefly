package org.firefly.transport.api.processor;

import org.firefly.model.rpc.response.JResponseBytes;
import org.firefly.model.transport.channel.interfice.JChannel;

public interface ConsumerProcessor {

    void handleResponse(JChannel channel, JResponseBytes response) throws Exception;
}
