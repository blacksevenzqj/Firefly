package org.firefly.rpc.consumer.proxy.future.listener;

import org.firefly.model.transport.channel.interfice.JChannel;

import java.util.EventListener;

/**
 * Listen on {@link JChannel}'s event.
 */
public interface JFutureListener<C> extends EventListener {

    void operationSuccess(C c) throws Exception;

    void operationFailure(C c, Throwable cause) throws Exception;
}
