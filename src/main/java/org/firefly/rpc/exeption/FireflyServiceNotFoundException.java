package org.firefly.rpc.exeption;

/**
 * The corresponding service was not be found.
 * For efficiency this exception will not have a stack trace.
 */
public class FireflyServiceNotFoundException extends FireflyRemoteException {

    private static final long serialVersionUID = -2277731243490443074L;

    public FireflyServiceNotFoundException() {}

    public FireflyServiceNotFoundException(String message) {
        super(message);
    }

    public FireflyServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public FireflyServiceNotFoundException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
