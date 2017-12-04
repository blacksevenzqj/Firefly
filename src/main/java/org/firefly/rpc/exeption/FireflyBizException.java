package org.firefly.rpc.exeption;

import java.net.SocketAddress;

/**
 * 业务异常
 */
public class FireflyBizException extends FireflyRemoteException {

    private static final long serialVersionUID = -3996155413840689423L;

    public FireflyBizException(Throwable cause, SocketAddress remoteAddress) {
        super(cause, remoteAddress);
    }

    public FireflyBizException(String message, SocketAddress remoteAddress) {
        super(message, remoteAddress);
    }

    public FireflyBizException(String message, Throwable cause, SocketAddress remoteAddress) {
        super(message, cause, remoteAddress);
    }
}
