package org.firefly.rpc.exeption;

import java.net.SocketAddress;

/**
 * 序列化/反序列化异常
 */
public class JupiterSerializationException extends JupiterRemoteException {

    private static final long serialVersionUID = -5079093080483380586L;

    public JupiterSerializationException() {}

    public JupiterSerializationException(SocketAddress remoteAddress) {
        super(remoteAddress);
    }

    public JupiterSerializationException(String message) {
        super(message);
    }

    public JupiterSerializationException(String message, SocketAddress remoteAddress) {
        super(message, remoteAddress);
    }

    public JupiterSerializationException(Throwable cause) {
        super(cause);
    }

    public JupiterSerializationException(Throwable cause, SocketAddress remoteAddress) {
        super(cause, remoteAddress);
    }
}
