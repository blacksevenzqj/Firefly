package org.firefly.transport.api.acceptor;

import org.firefly.transport.api.Transporter;
import org.firefly.transport.api.configuration.group.JConfigGroup;
import org.firefly.transport.api.processor.ProviderProcessor;

import java.net.SocketAddress;

/**
 * Server acceptor.
 * 注意 JAcceptor 单例即可, 不要创建多个实例.
 */
public interface JAcceptor extends Transporter {

    /**
     * Local address.
     */
    SocketAddress localAddress();

    /**
     * Returns bound port.
     */
    int boundPort();

    /**
     * Acceptor options [parent, child].
     */
    JConfigGroup configGroup();

    /**
     * Binds the rpc processor.
     */
    void withProcessor(ProviderProcessor processor);

    /**
     * Start the server and wait until the server socket is closed.
     */
    void start() throws InterruptedException;

    /**
     * Start the server.
     */
    void start(boolean sync) throws InterruptedException;

    /**
     * Shutdown the server gracefully.
     */
    void shutdownGracefully();
}
