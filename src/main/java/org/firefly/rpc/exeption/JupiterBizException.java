package org.firefly.rpc.exeption;

import java.net.SocketAddress;

/**
 * 业务异常
 */
public class JupiterBizException extends JupiterRemoteException {

    private static final long serialVersionUID = -3996155413840689423L;

    public JupiterBizException(Throwable cause, SocketAddress remoteAddress) {
        super(cause, remoteAddress);
    }

    public JupiterBizException(String message, SocketAddress remoteAddress) {
        super(message, remoteAddress);
    }

    public JupiterBizException(String message, Throwable cause, SocketAddress remoteAddress) {
        super(message, cause, remoteAddress);
    }
}
