package org.firefly.rpc.exeption;

import org.firefly.model.transport.configuration.Status;

import java.net.SocketAddress;

public class FireflyTimeoutException extends FireflyRemoteException {

    private static final long serialVersionUID = 8768621104391094458L;

    private final Status status;

    public FireflyTimeoutException(SocketAddress remoteAddress, Status status) {
        super(remoteAddress);
        this.status = status;
    }

    public FireflyTimeoutException(Throwable cause, SocketAddress remoteAddress, Status status) {
        super(cause, remoteAddress);
        this.status = status;
    }

    public FireflyTimeoutException(String message, SocketAddress remoteAddress, Status status) {
        super(message, remoteAddress);
        this.status = status;
    }

    public FireflyTimeoutException(String message, Throwable cause, SocketAddress remoteAddress, Status status) {
        super(message, cause, remoteAddress);
        this.status = status;
    }

    public Status status() {
        return status;
    }

    @Override
    public String toString() {
        return "TimeoutException{" +
                "status=" + status +
                '}';
    }
}
