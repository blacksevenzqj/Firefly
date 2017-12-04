package org.firefly.transport.netty.acceptor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;
import org.firefly.common.util.constant.JConstants;
import org.firefly.model.transport.configuration.netty.JOption;
import org.firefly.transport.api.configuration.template.JConfig;
import org.firefly.transport.api.processor.ProviderProcessor;
import org.firefly.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;
import org.firefly.transport.netty.handler.acceptor.provider.ProviderAcceptorHandler;
import org.firefly.transport.netty.handler.codec.decoder.ProtocolDecoder;
import org.firefly.transport.netty.handler.codec.encoder.ProtocolEncoder;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * tcp acceptor based on netty.
 *
 * <pre>
 * *********************************************************************
 *            I/O Request                       I/O Response
 *                 │                                 △
 *                                                   │
 *                 │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┼ ─ ─ ─ ─ ─ ─ ─ ─
 * │               │                                                  │
 *                                                   │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
*             IdleStateChecker#inBound                         IdleStateChecker#outBound
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘   │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐                                     │
 *             AcceptorIdleStateTrigger                      │
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                                     │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐   │
 *                  ProtocolDecoder                                ProtocolEncoder
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘   │
 *                 │                                 │
 * │                                                                  │
 *                 │                                 │
 * │  ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐                                     │
 *                AcceptorHandler                          │
 * │  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                                     │
 *                 │                                 │
 * │                    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐                     │
 *                 ▽                                 │
 * │               ─ ─ ▷│       Processor       ├ ─ ─▷                │
 *
 * │                    └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘                     │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 * </pre>
 */
public class ProviderServerNettyTcpAcceptor extends NettyTcpAcceptor {

    public static final int DEFAULT_ACCEPTOR_PORT = 18090;

    // 读超时
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final ProtocolEncoder encoder = new ProtocolEncoder();
    private final ProviderAcceptorHandler handler = new ProviderAcceptorHandler();

    public ProviderServerNettyTcpAcceptor() {
        super(DEFAULT_ACCEPTOR_PORT);
    }

    public ProviderServerNettyTcpAcceptor(int port) {
        super(port);
    }

    public ProviderServerNettyTcpAcceptor(SocketAddress localAddress) {
        super(localAddress);
    }

    public ProviderServerNettyTcpAcceptor(int port, int nWorkers) {
        super(port, nWorkers);
    }

    public ProviderServerNettyTcpAcceptor(SocketAddress localAddress, int nWorkers) {
        super(localAddress, nWorkers);
    }

    public ProviderServerNettyTcpAcceptor(int port, boolean isNative) {
        super(port, isNative);
    }

    public ProviderServerNettyTcpAcceptor(SocketAddress localAddress, boolean isNative) {
        super(localAddress, isNative);
    }

    public ProviderServerNettyTcpAcceptor(int port, int nWorkers, boolean isNative) {
        super(port, nWorkers, isNative);
    }

    public ProviderServerNettyTcpAcceptor(SocketAddress localAddress, int nWorkers, boolean isNative) {
        super(localAddress, nWorkers, isNative);
    }

    @Override
    protected void doInit() {
        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 32768);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        ServerBootstrap boot = bootstrap();

        initChannelFactory();

        boot.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(
                        new IdleStateHandler(JConstants.READER_IDLE_TIME_SECONDS, 0, 0, TimeUnit.SECONDS),
                        idleStateTrigger,
                        new ProtocolDecoder(),
                        encoder,
                        handler);
            }
        });

        setOptions();

        return boot.bind(localAddress);
    }

    @Override
    public void withProcessor(ProviderProcessor processor) {
        handler.processor(checkNotNull(processor, "processor"));
    }
}
