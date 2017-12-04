package org.firefly.transport.netty.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.ConcurrentSet;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.SystemClock;
import org.firefly.common.util.SystemPropertyUtil;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.registry.nonack.ClientNettyConnectorMessageNonAck;
import org.firefly.model.transport.ack.Acknowledge;
import org.firefly.model.transport.configuration.netty.JOption;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.model.transport.protocol.JProtocolHeader;
import org.firefly.registry.api.AbstractRegistryService;
import org.firefly.serialization.SerializerType;
import org.firefly.transport.api.connector.connection.JConnection;
import org.firefly.transport.api.exception.ConnectFailedException;
import org.firefly.transport.netty.connector.connection.ClientToRegistryNettyConnection;
import org.firefly.transport.netty.handler.codec.decoder.client.ClientPublishSubscriptionMessageDecoder;
import org.firefly.transport.netty.handler.codec.encoder.AcknowledgeEncoder;
import org.firefly.transport.netty.handler.codec.encoder.PublishSubscriptionMessageEncoder;
import org.firefly.transport.netty.handler.connector.ConnectionWatchdog;
import org.firefly.transport.netty.handler.connector.ConnectorIdleStateTrigger;
import org.firefly.transport.netty.handler.connector.ClientPublishSubscriptionMessageHandler;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import static org.firefly.common.util.Preconditions.checkNotNull;
import static org.firefly.common.util.StackTraceUtil.stackTrace;

public final class ClientToRegistryNettyConnector extends NettyTcpConnector {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientToRegistryNettyConnector.class);

    private static final AttributeKey<ConcurrentSet<RegisterMeta.ServiceMeta>> C_SUBSCRIBE_KEY =
            AttributeKey.valueOf("client.subscribed");
    private static final AttributeKey<ConcurrentSet<RegisterMeta>> C_PUBLISH_KEY =
            AttributeKey.valueOf("client.published");

    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<Long, ClientNettyConnectorMessageNonAck> messagesNonAck = Maps.newConcurrentMap();

    // handlers
    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final PublishSubscriptionMessageEncoder encoder = new PublishSubscriptionMessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();
    private final ClientPublishSubscriptionMessageHandler handler = new ClientPublishSubscriptionMessageHandler(this);

    // 序列化/反序列化方式
    private final SerializerType serializerType;

    {
        SerializerType expected = SerializerType.parse(SystemPropertyUtil.get("firefly.registry.default.serializer_type"));
        serializerType = expected == null ? SerializerType.getDefault() : expected;
    }

    private final AbstractRegistryService registryService;

    // 每个ConfigClient只保留一个有效channel
    private volatile Channel channel;

    public ClientToRegistryNettyConnector(AbstractRegistryService registryService) {
        this(registryService, 1);
    }

    public ClientToRegistryNettyConnector(AbstractRegistryService registryService, int nWorkers) {
        super(nWorkers);
        this.registryService = checkNotNull(registryService, "registryService");
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(3));
        // channel factory
        initChannelFactory();
    }

    /**
     * ConfigClient不支持异步连接行为, async参数无效
     */
    @Override
    public JConnection connect(UnresolvedAddress address, boolean async) {
        setOptions();

        final Bootstrap boot = bootstrap();
        final SocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());

        // 重连watchdog
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, socketAddress, null) {

            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                        this,
                        new IdleStateHandler(0, JConstants.WRITER_IDLE_TIME_SECONDS, 0, TimeUnit.SECONDS),
                        idleStateTrigger,
                        new ClientPublishSubscriptionMessageDecoder(),
                        encoder,
                        ackEncoder,
                        handler
                };
            }
        };

        try {
            ChannelFuture future;
            System.out.println("DefaultRegistry JConnection connect(UnresolvedAddress address, boolean async) synchronized synchronized 之前");
            synchronized (bootstrapLock()) {

                System.out.println("DefaultRegistry JConnection connect(UnresolvedAddress address, boolean async) synchronized synchronized 进锁了");

                boot.handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = boot.connect(socketAddress);
            }

            System.out.println("DefaultRegistry JConnection connect(UnresolvedAddress address, boolean async) synchronized synchronized 结束");

            // 以下代码在synchronized同步块外面是安全的
            future.sync();
            channel = future.channel();
        } catch (Throwable t) {
            throw new ConnectFailedException("connects to [" + address + "] fails", t);
        }

        // 服务提供者、消费者 连接 注册服务 后返回的 JConnection实例。
        // DefaultRegistryService 的 connectToRegistryServer方法 没有对 ClientToRegistryNettyConnector的connect方法返回的JConnection实例做处理。
        return new ClientToRegistryNettyConnection(address, watchdog);
    }

    /**
     * Sent the subscription information to registry server.
     */
    public void doSubscribe(RegisterMeta.ServiceMeta serviceMeta) {
        PublishSubscriptionMessage msg = new PublishSubscriptionMessage(serializerType.value());
        msg.messageCode(JProtocolHeader.SUBSCRIBE_SERVICE);
        msg.data(serviceMeta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachSubscribeEventOnChannel(serviceMeta, ch)) {
            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            ClientNettyConnectorMessageNonAck msgNonAck = new ClientNettyConnectorMessageNonAck(msg);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }
    }

    /**
     * Publishing service to registry server.
     */
    public void doRegister(RegisterMeta meta) {

        System.out.println("DefaultRegistry doRegister(RegisterMeta meta) Start");

        PublishSubscriptionMessage msg = new PublishSubscriptionMessage(serializerType.value());
        msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
        msg.data(meta);

        Channel ch = channel;
        // 与MessageHandler#channelActive()中的write有竞争
        if (attachPublishEventOnChannel(meta, ch)) {

            System.out.println("DefaultRegistry doRegister(RegisterMeta meta) Running");

            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            ClientNettyConnectorMessageNonAck msgNonAck = new ClientNettyConnectorMessageNonAck(msg);
            messagesNonAck.put(msgNonAck.id, msgNonAck);
        }

        System.out.println("DefaultRegistry doRegister(RegisterMeta meta) End");

    }

    /**
     * Notify to registry server unpublish corresponding service.
     */
    public void doUnregister(final RegisterMeta meta) {
        PublishSubscriptionMessage msg = new PublishSubscriptionMessage(serializerType.value());
        msg.messageCode(JProtocolHeader.PUBLISH_CANCEL_SERVICE);
        msg.data(meta);

        channel.writeAndFlush(msg)
                .addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            Channel ch = future.channel();
                            if (ch.isActive()) {
                                ch.pipeline().fireExceptionCaught(future.cause());
                            } else {
                                if (logger.isWarnEnabled()) {
                                    logger.warn("Unregister {} fail because of channel is inactive: {}.",
                                            meta, stackTrace(future.cause()));
                                }
                            }
                        }
                    }
                });

        ClientNettyConnectorMessageNonAck msgNonAck = new ClientNettyConnectorMessageNonAck(msg);
        messagesNonAck.put(msgNonAck.id, msgNonAck);
    }

    public void handleAcknowledge(Acknowledge ack) {
        messagesNonAck.remove(ack.sequence());
    }

    // 在channel打标记(发布过的服务)
    public static boolean attachPublishEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(C_PUBLISH_KEY);
        ConcurrentSet<RegisterMeta> registerMetaSet = attr.get();
        if (registerMetaSet == null) {
            ConcurrentSet<RegisterMeta> newRegisterMetaSet = new ConcurrentSet<>();
            registerMetaSet = attr.setIfAbsent(newRegisterMetaSet);
            if (registerMetaSet == null) {
                registerMetaSet = newRegisterMetaSet;
            }
        }

        return registerMetaSet.add(meta);
    }

    // 在channel打标记(订阅过的服务)
    public static boolean attachSubscribeEventOnChannel(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta.ServiceMeta>> attr = channel.attr(C_SUBSCRIBE_KEY);
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = attr.get();
        if (serviceMetaSet == null) {
            ConcurrentSet<RegisterMeta.ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
            serviceMetaSet = attr.setIfAbsent(newServiceMetaSet);
            if (serviceMetaSet == null) {
                serviceMetaSet = newServiceMetaSet;
            }
        }

        return serviceMetaSet.add(serviceMeta);
    }

    private class AckTimeoutScanner implements Runnable {

        @SuppressWarnings("all")
        @Override
        public void run() {
            for (;;) {
                try {
                    for (ClientNettyConnectorMessageNonAck m : messagesNonAck.values()) {
                        if (SystemClock.millisClock().now() - m.timestamp > TimeUnit.SECONDS.toMillis(10)) {

                            // 移除
                            if (messagesNonAck.remove(m.id) == null) {
                                continue;
                            }

                            ClientNettyConnectorMessageNonAck msgNonAck = new ClientNettyConnectorMessageNonAck(m.msg);
                            messagesNonAck.put(msgNonAck.id, msgNonAck);
                            channel.writeAndFlush(m.msg)
                                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                        }
                    }

                    Thread.sleep(300);
                } catch (Throwable t) {
                    logger.error("An exception was caught while scanning the timeout acknowledges {}.", stackTrace(t));
                }
            }
        }
    }

    {
        Thread t = new Thread(new AckTimeoutScanner(), "ack.timeout.scanner");
        t.setDaemon(true);
        t.start();
    }



    public AbstractRegistryService getRegistryService() {
        return registryService;
    }
    public ConcurrentMap<Long, ClientNettyConnectorMessageNonAck> getMessagesNonAck() {
        return messagesNonAck;
    }
    public SerializerType getSerializerType() {
        return serializerType;
    }
    public Channel setAndGetClientNettyConnectorChannel(Channel channel) {
        this.channel = channel;
        return this.channel;
    }




}
