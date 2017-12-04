package org.firefly.rpc.exeption;

/**
 * Usually it means that the server can not handle the new request.
 * For efficiency this exception will not have a stack trace.
 */
public class FireflyServerBusyException extends FireflyRemoteException {

    private static final long serialVersionUID = 4812626729436624336L;

    public FireflyServerBusyException() {}

    public FireflyServerBusyException(String message) {
        super(message);
    }

    public FireflyServerBusyException(String message, Throwable cause) {
        super(message, cause);
    }

    public FireflyServerBusyException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
