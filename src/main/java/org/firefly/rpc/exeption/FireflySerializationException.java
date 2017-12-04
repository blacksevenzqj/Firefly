package org.firefly.rpc.exeption;

import java.net.SocketAddress;

/**
 * 序列化/反序列化异常
 */
public class FireflySerializationException extends FireflyRemoteException {

    private static final long serialVersionUID = -5079093080483380586L;

    public FireflySerializationException() {}

    public FireflySerializationException(SocketAddress remoteAddress) {
        super(remoteAddress);
    }

    public FireflySerializationException(String message) {
        super(message);
    }

    public FireflySerializationException(String message, SocketAddress remoteAddress) {
        super(message, remoteAddress);
    }

    public FireflySerializationException(Throwable cause) {
        super(cause);
    }

    public FireflySerializationException(Throwable cause, SocketAddress remoteAddress) {
        super(cause, remoteAddress);
    }
}
