package org.firefly.transport.netty.acceptor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.firefly.common.util.constant.JConstants;
import org.firefly.transport.api.Transporter;
import org.firefly.transport.api.configuration.*;
import org.firefly.transport.api.configuration.group.JConfigGroup;
import org.firefly.transport.api.configuration.group.NettyTcpConfigGroup;
import org.firefly.transport.netty.channelfactory.NativeSupport;
import org.firefly.transport.netty.channelfactory.TcpChannelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

public abstract class NettyTcpAcceptor extends NettyAcceptor {

    private static final Logger logger = LoggerFactory.getLogger(NettyTcpAcceptor.class);

    private final boolean isNative; // use native transport
    private final NettyTcpConfigGroup configGroup = new NettyTcpConfigGroup();

    public NettyTcpAcceptor(int port) {
        super(Transporter.Protocol.TCP, new InetSocketAddress(port));
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress) {
        super(Transporter.Protocol.TCP, localAddress);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(int port, int nWorkers) {
        super(Transporter.Protocol.TCP, new InetSocketAddress(port), nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(int port, int nBosses, int nWorkers) {
        super(Transporter.Protocol.TCP, new InetSocketAddress(port), nBosses, nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nWorkers) {
        super(Transporter.Protocol.TCP, localAddress, nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nBosses, int nWorkers) {
        super(Transporter.Protocol.TCP, localAddress, nBosses, nWorkers);
        isNative = false;
        init();
    }

    public NettyTcpAcceptor(int port, boolean isNative) {
        super(Transporter.Protocol.TCP, new InetSocketAddress(port));
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, boolean isNative) {
        super(Transporter.Protocol.TCP, localAddress);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(int port, int nWorkers, boolean isNative) {
        super(Transporter.Protocol.TCP, new InetSocketAddress(port), nWorkers);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(int port, int nBosses, int nWorkers, boolean isNative) {
        super(Transporter.Protocol.TCP, new InetSocketAddress(port), nBosses, nWorkers);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nWorkers, boolean isNative) {
        super(Transporter.Protocol.TCP, localAddress, nWorkers);
        this.isNative = isNative;
        init();
    }

    public NettyTcpAcceptor(SocketAddress localAddress, int nBosses, int nWorkers, boolean isNative) {
        super(Transporter.Protocol.TCP, localAddress, nBosses, nWorkers);
        this.isNative = isNative;
        init();
    }

    @Override
    protected void setOptions() {
        super.setOptions();

        ServerBootstrap boot = bootstrap();

        // parent options
        ParentConfig parent = configGroup.parent();
        boot.option(ChannelOption.SO_BACKLOG, parent.getBacklog());
        boot.option(ChannelOption.SO_REUSEADDR, parent.isReuseAddress());
        if (parent.getRcvBuf() > 0) {
            boot.option(ChannelOption.SO_RCVBUF, parent.getRcvBuf());
        }

        // child options
        ChildConfig child = configGroup.child();
        boot.childOption(ChannelOption.SO_REUSEADDR, child.isReuseAddress())
                .childOption(ChannelOption.SO_KEEPALIVE, child.isKeepAlive())
                .childOption(ChannelOption.TCP_NODELAY, child.isTcpNoDelay())
                .childOption(ChannelOption.ALLOW_HALF_CLOSURE, child.isAllowHalfClosure());
        if (child.getRcvBuf() > 0) {
            boot.childOption(ChannelOption.SO_RCVBUF, child.getRcvBuf());
        }
        if (child.getSndBuf() > 0) {
            boot.childOption(ChannelOption.SO_SNDBUF, child.getSndBuf());
        }
        if (child.getLinger() > 0) {
            boot.childOption(ChannelOption.SO_LINGER, child.getLinger());
        }
        if (child.getIpTos() > 0) {
            boot.childOption(ChannelOption.IP_TOS, child.getIpTos());
        }
        int bufLowWaterMark = child.getWriteBufferLowWaterMark();
        int bufHighWaterMark = child.getWriteBufferHighWaterMark();
        if (bufLowWaterMark >= 0 && bufHighWaterMark > 0) {
            WriteBufferWaterMark waterMark = new WriteBufferWaterMark(bufLowWaterMark, bufHighWaterMark);
            boot.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, waterMark);
        }
    }

    @Override
    public JConfigGroup configGroup() {
        return configGroup;
    }

    @Override
    public void start() throws InterruptedException {
        start(true);
    }

    @Override
    public void start(boolean sync) throws InterruptedException {
        // wait until the server socket is bind succeed.
        ChannelFuture future = bind(localAddress).sync();

        if (logger.isInfoEnabled()) {
            logger.info("Jupiter TCP server start" + (sync ? ", and waits until the server socket closed." : ".")
                    + JConstants.NEWLINE + " {}.", toString());
        }

        if (sync) {
            logger.info("NettyTcpAcceptor start(boolean sync:true)!!!");
            // wait until the server socket is closed.
            future.channel().closeFuture().sync();
        }
    }

    @Override
    public void setIoRatio(int bossIoRatio, int workerIoRatio) {
        EventLoopGroup boss = boss();
        if (boss instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) boss).setIoRatio(bossIoRatio);
        } else if (boss instanceof KQueueEventLoopGroup) {
            ((KQueueEventLoopGroup) boss).setIoRatio(bossIoRatio);
        } else if (boss instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) boss).setIoRatio(bossIoRatio);
        }

        EventLoopGroup worker = worker();
        if (worker instanceof EpollEventLoopGroup) {
            ((EpollEventLoopGroup) worker).setIoRatio(workerIoRatio);
        } else if (worker instanceof KQueueEventLoopGroup) {
            ((KQueueEventLoopGroup) worker).setIoRatio(workerIoRatio);
        } else if (worker instanceof NioEventLoopGroup) {
            ((NioEventLoopGroup) worker).setIoRatio(workerIoRatio);
        }
    }

    @Override
    protected EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory) {
        TcpChannelProvider.SocketType socketType = socketType();
        switch (socketType) {
            case NATIVE_EPOLL:
                return new EpollEventLoopGroup(nThreads, tFactory);
            case NATIVE_KQUEUE:
                return new KQueueEventLoopGroup(nThreads, tFactory);
            case JAVA_NIO:
                return new NioEventLoopGroup(nThreads, tFactory);
            default:
                throw new IllegalStateException("invalid socket type: " + socketType);
        }
    }

    protected void initChannelFactory() {
        TcpChannelProvider.SocketType socketType = socketType();
        switch (socketType) {
            case NATIVE_EPOLL:
                bootstrap().channelFactory(TcpChannelProvider.NATIVE_EPOLL_ACCEPTOR);
                break;
            case NATIVE_KQUEUE:
                bootstrap().channelFactory(TcpChannelProvider.NATIVE_KQUEUE_ACCEPTOR);
                break;
            case JAVA_NIO:
                bootstrap().channelFactory(TcpChannelProvider.JAVA_NIO_ACCEPTOR);
                break;
            default:
                throw new IllegalStateException("invalid socket type: " + socketType);
        }
    }

    private TcpChannelProvider.SocketType socketType() {
        if (isNative && NativeSupport.isNativeEPollAvailable()) {
            // netty provides the native socket transport for Linux using JNI.
            return TcpChannelProvider.SocketType.NATIVE_EPOLL;
        }
        if (isNative && NativeSupport.isNativeKQueueAvailable()) {
            // netty provides the native socket transport for BSD systems such as MacOS using JNI.
            return TcpChannelProvider.SocketType.NATIVE_KQUEUE;
        }
        return TcpChannelProvider.SocketType.JAVA_NIO;
    }

    @Override
    public String toString() {
        return "Socket address:[" + localAddress + ']'
                + ", socket type: " + socketType()
                + JConstants.NEWLINE
                + bootstrap();
    }
}
