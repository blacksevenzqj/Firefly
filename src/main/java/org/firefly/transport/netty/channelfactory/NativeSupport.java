package org.firefly.transport.netty.channelfactory;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;

/**
 * Netty provides the native socket transport using JNI.
 * This transport has higher performance and produces less garbage.
 */
public final class NativeSupport {

    /**
     * The native socket transport for Linux using JNI.
     */
    public static boolean isNativeEPollAvailable() {
        return Epoll.isAvailable();
    }

    /**
     * The native socket transport for BSD systems such as MacOS using JNI.
     */
    public static boolean isNativeKQueueAvailable() {
        return KQueue.isAvailable();
    }
}
