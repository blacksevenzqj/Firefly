package org.firefly.rpc.exeption;

import java.net.SocketAddress;

public class FireflyRemoteException extends RuntimeException {

    private static final long serialVersionUID = -6516335527982400712L;

    private final SocketAddress remoteAddress;

    public FireflyRemoteException() {
        this.remoteAddress = null;
    }

    public FireflyRemoteException(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public FireflyRemoteException(Throwable cause) {
        super(cause);
        this.remoteAddress = null;
    }

    public FireflyRemoteException(Throwable cause, SocketAddress remoteAddress) {
        super(cause);
        this.remoteAddress = remoteAddress;
    }

    public FireflyRemoteException(String message) {
        super(message);
        this.remoteAddress = null;
    }

    public FireflyRemoteException(String message, SocketAddress remoteAddress) {
        super(message);
        this.remoteAddress = remoteAddress;
    }

    public FireflyRemoteException(String message, Throwable cause) {
        super(message, cause);
        this.remoteAddress = null;
    }

    public FireflyRemoteException(String message, Throwable cause, SocketAddress remoteAddress) {
        super(message, cause);
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}
