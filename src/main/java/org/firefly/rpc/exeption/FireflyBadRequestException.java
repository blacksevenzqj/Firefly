package org.firefly.rpc.exeption;

/**
 * Request content deserialize failure.
 * For efficiency this exception will not have a stack trace.
 */
public class FireflyBadRequestException extends FireflyRemoteException {

    private static final long serialVersionUID = -6603241073638657127L;

    public FireflyBadRequestException() {}

    public FireflyBadRequestException(String message) {
        super(message);
    }

    public FireflyBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public FireflyBadRequestException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
