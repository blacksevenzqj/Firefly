package org.firefly.transport.netty.acceptor;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatcher;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ConcurrentSet;
import org.firefly.common.util.exception.ExceptionUtil;
import org.firefly.common.util.Pair;
import org.firefly.common.util.SystemClock;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.interfice.Function;
import org.firefly.common.util.SystemPropertyUtil;
import org.firefly.common.util.internal.Lists;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.registry.ConfigWithVersion;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.registry.nonack.RegistryServerMessageNonAck;
import org.firefly.model.transport.ack.Acknowledge;
import org.firefly.model.transport.configuration.netty.JOption;
import org.firefly.model.transport.protocol.JProtocolHeader;
import org.firefly.registry.api.registryserver.RegistryServer;
import org.firefly.registry.defaults.context.RegisterInfoContext;
import org.firefly.serialization.SerializerType;
import org.firefly.transport.api.configuration.template.JConfig;
import org.firefly.transport.netty.handler.acceptor.AcceptorIdleStateTrigger;
import org.firefly.transport.netty.handler.acceptor.registryserver.RegistryServerPublishSubscriptionMessageHandler;
import org.firefly.transport.netty.handler.codec.decoder.registryserver.RegistryServerPublishSubscriptionMessageDecoder;
import org.firefly.transport.netty.handler.codec.encoder.AcknowledgeEncoder;
import org.firefly.transport.netty.handler.codec.encoder.PublishSubscriptionMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * The RegistryServer of registration center.
 * 1、所有信息均在内存中, 不持久化.
 * 2、provider(client) 断线时所有该 provider 发布过的服务会被 RegistryServer 清除并通知订阅者, 重新建立连接后 provider 会自动重新发布相关服务,
 * 并且 RegistryServer 会重新推送服务给订阅者.
 * 3、consumer(client)断线时所有该consumer订阅过的服务会被server清除, 重新建立连接后consumer会自动重新订阅相关服务.
 */
public final class DefaultRegistryServerNettyTcpAcceptor extends NettyTcpAcceptor implements RegistryServer {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRegistryServerNettyTcpAcceptor.class);

    public static final AttributeKey<ConcurrentSet<RegisterMeta.ServiceMeta>> S_SUBSCRIBE_KEY =
            AttributeKey.valueOf("server.subscribed");

    public static final AttributeKey<ConcurrentSet<RegisterMeta>> S_PUBLISH_KEY =
            AttributeKey.valueOf("server.published");

    // 注册信息
    private final RegisterInfoContext registerInfoContext = new RegisterInfoContext();
    // 订阅者
    private final ChannelGroup subscriberChannels = new DefaultChannelGroup("subscribers", GlobalEventExecutor.INSTANCE);
    // 没收到对端ack确认, 需要重发的消息
    private final ConcurrentMap<String, RegistryServerMessageNonAck> messagesNonAck = Maps.newConcurrentMap();

    // handlers
    private final AcceptorIdleStateTrigger idleStateTrigger = new AcceptorIdleStateTrigger();
    private final PublishSubscriptionMessageEncoder encoder = new PublishSubscriptionMessageEncoder();
    private final AcknowledgeEncoder ackEncoder = new AcknowledgeEncoder();
    private RegistryServerPublishSubscriptionMessageHandler handler = new RegistryServerPublishSubscriptionMessageHandler(this);

    // 序列化/反序列化方式
    private final SerializerType serializerType;

    {
        SerializerType expected = SerializerType.parse(SystemPropertyUtil.get("firefly.registry.default.serializer_type"));
        serializerType = expected == null ? SerializerType.getDefault() : expected;
    }

    public DefaultRegistryServerNettyTcpAcceptor(int port) {
        super(port);
    }

    public DefaultRegistryServerNettyTcpAcceptor(SocketAddress address) {
        super(address);
    }

    public DefaultRegistryServerNettyTcpAcceptor(int port, int nWorkers) {
        super(port, nWorkers);
    }

    public DefaultRegistryServerNettyTcpAcceptor(SocketAddress address, int nWorkers) {
        super(address, nWorkers);
    }

    @Override
    protected void doInit() {
        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.SO_BACKLOG, 1024);
        parent.setOption(JOption.SO_REUSEADDR, true);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.SO_REUSEADDR, true);
    }

    // RegistryServer 的接口方法：
    @Override
    public void startRegistryServer() {
        try {
            start();
        } catch (InterruptedException e) {
            ExceptionUtil.throwException(e);
        }
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
                        new RegistryServerPublishSubscriptionMessageDecoder(),
                        encoder,
                        ackEncoder,
                        handler);
            }
        });

        setOptions();

        return boot.bind(localAddress);
    }

    public List<String> listPublisherHosts() {
        List<RegisterMeta.Address> fromList = registerInfoContext.listPublisherHosts();

        return Lists.transform(fromList, new Function<RegisterMeta.Address, String>() {

            @Override
            public String apply(RegisterMeta.Address input) {
                return input.getHost();
            }
        });
    }

    public List<String> listSubscriberAddresses() {
        List<String> hosts = Lists.newArrayList();
        for (Channel ch : subscriberChannels) {
            SocketAddress address = ch.remoteAddress();
            if (address instanceof InetSocketAddress) {
                String host = ((InetSocketAddress) address).getAddress().getHostAddress();
                int port = ((InetSocketAddress) address).getPort();
                hosts.add(new RegisterMeta.Address(host, port).toString());
            }
        }
        return hosts;
    }

    public List<String> listAddressesByService(String group, String serviceProviderName, String version) {
        RegisterMeta.ServiceMeta serviceMeta = new RegisterMeta.ServiceMeta(group, serviceProviderName, version);
        List<RegisterMeta.Address> fromList = registerInfoContext.listAddressesByService(serviceMeta);

        return Lists.transform(fromList, new Function<RegisterMeta.Address, String>() {

            @Override
            public String apply(RegisterMeta.Address input) {
                return input.toString();
            }
        });
    }

    public List<String> listServicesByAddress(String host, int port) {
        RegisterMeta.Address address = new RegisterMeta.Address(host, port);
        List<RegisterMeta.ServiceMeta> fromList = registerInfoContext.listServicesByAddress(address);

        return Lists.transform(fromList, new Function<RegisterMeta.ServiceMeta, String>() {

            @Override
            public String apply(RegisterMeta.ServiceMeta input) {
                return input.toString();
            }
        });
    }

    // 添加指定机器指定服务, 然后全量发布到所有客户端
    public void handlePublish(RegisterMeta meta, Channel channel) {

        logger.info("Publish {} on channel{}.", meta, channel);

        // 在Channel上打标记：以发布过的服务。
        attachPublishEventOnChannel(meta, channel);

        final RegisterMeta.ServiceMeta serviceMeta = meta.getServiceMeta();

        // 向“注册服务”所维护的发布服务集合上下文RegisterInfoContext中添加本次“服务提供者”发布的服务信息：
        // ConcurrentMap<RegisterMeta.ServiceMeta, ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>>> putIfAbsent
        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            if (config.getConfig().putIfAbsent(meta.getAddress(), meta) == null) {
                // 向“注册服务”所维护的发布服务集合上下文RegisterInfoContext中添加本次“服务提供者”发布的服务信息：
                // ConcurrentMap<RegisterMeta.Address, ConcurrentSet<RegisterMeta.ServiceMeta>> putIfAbsent
                // ConcurrentSet<RegisterMeta.ServiceMeta>> add
                registerInfoContext.getServiceMeta(meta.getAddress()).add(serviceMeta);

                final PublishSubscriptionMessage msg = new PublishSubscriptionMessage(serializerType.value());
                msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
                msg.version(config.newVersion()); // 版本号+1
                msg.data(Pair.of(serviceMeta, meta));

                // 注意：ChannelGroup 为“客户端”订阅的Channel的集合：向每个订阅过本次“服务提供者”发布的服务相匹配的 “消费者”关联Channel发送消息
                subscriberChannels.writeAndFlush(msg, new ChannelMatcher() {

                    @Override
                    public boolean matches(Channel channel) {
                        boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, channel);
                        if (doSend) {
                            RegistryServerMessageNonAck msgNonAck = new RegistryServerMessageNonAck(serviceMeta, msg, channel);
                            // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                            // 要求“消费者”收到消息后，发送回复ACK消息给“注册服务”删除ConcurrentMap<String, MessageNonAck>中相应的MessageNonAck
                            messagesNonAck.put(msgNonAck.id, msgNonAck);
                        }
                        return doSend;
                    }
                });
            }
        }
    }

    // 删除指定机器指定服务, 然后全量发布到所有客户端
    public void handlePublishCancel(RegisterMeta meta, Channel channel) {

        logger.info("Cancel publish {} on channel{}.", meta, channel);

        attachPublishCancelEventOnChannel(meta, channel);

        final RegisterMeta.ServiceMeta serviceMeta = meta.getServiceMeta();
        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config =
                registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        synchronized (registerInfoContext.publishLock(config)) {
            // putIfAbsent和config.newVersion()需要是原子操作, 所以这里加锁
            RegisterMeta.Address address = meta.getAddress();
            RegisterMeta data = config.getConfig().remove(address);
            if (data != null) {
                registerInfoContext.getServiceMeta(address).remove(serviceMeta);

                final PublishSubscriptionMessage msg = new PublishSubscriptionMessage(serializerType.value());
                msg.messageCode(JProtocolHeader.PUBLISH_CANCEL_SERVICE);
                msg.version(config.newVersion()); // 版本号+1
                msg.data(Pair.of(serviceMeta, data));

                subscriberChannels.writeAndFlush(msg, new ChannelMatcher() {

                    @Override
                    public boolean matches(Channel channel) {
                        boolean doSend = isChannelSubscribeOnServiceMeta(serviceMeta, channel);
                        if (doSend) {
                            RegistryServerMessageNonAck msgNonAck = new RegistryServerMessageNonAck(serviceMeta, msg, channel);
                            // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发
                            messagesNonAck.put(msgNonAck.id, msgNonAck);
                        }
                        return doSend;
                    }
                });
            }
        }
    }

    // 订阅服务
    public void handleSubscribe(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {

        logger.info("Subscribe {} on channel{}.", serviceMeta, channel);

        attachSubscribeEventOnChannel(serviceMeta, channel);

        // 将于“消费者”关联的Channel添加进“注册服务”所维护的ChannelGroup中
        subscriberChannels.add(channel);

        // 从“注册服务”所维护的发布服务集合上下文RegisterInfoContext中获取本次“消费者”订阅的服务信息集合：
        // ConcurrentMap<RegisterMeta.ServiceMeta, ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>>>
        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config = registerInfoContext.getRegisterMeta(serviceMeta);
        if (config.getConfig().isEmpty()) {
            return;
        }

        final PublishSubscriptionMessage msg = new PublishSubscriptionMessage(serializerType.value());
        msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
        msg.version(config.getVersion()); // 版本号
        List<RegisterMeta> registerMetaList = Lists.newArrayList(config.getConfig().values());
        // 每次发布服务都是当前meta的全量信息
        msg.data(Pair.of(serviceMeta, registerMetaList));

        RegistryServerMessageNonAck msgNonAck = new RegistryServerMessageNonAck(serviceMeta, msg, channel);
        // 收到ack后会移除当前key(参见handleAcknowledge), 否则超时超时重发。
        // 要求“消费者”收到消息后，发送回复ACK消息给“注册服务”删除ConcurrentMap<String, MessageNonAck>中相应的MessageNonAck
        messagesNonAck.put(msgNonAck.id, msgNonAck);
        // 向与“消费者”关联的Channel中发送订阅的服务消息
        channel.writeAndFlush(msg);
    }

    // 处理ack
    public void handleAcknowledge(Acknowledge ack, Channel channel) {
        messagesNonAck.remove(key(ack.sequence(), channel));
    }

    // 发布Provider下线的通告
    public void handleOfflineNotice(RegisterMeta.Address address) {

        logger.info("OfflineNotice on {}.", address);

        PublishSubscriptionMessage msg = new PublishSubscriptionMessage(serializerType.value());
        msg.messageCode(JProtocolHeader.OFFLINE_NOTICE);
        msg.data(address);
        subscriberChannels.writeAndFlush(msg);
    }

    public static String key(long sequence, Channel channel) {
        return String.valueOf(sequence) + '-' + channel.id().asShortText();
    }

    // 在与“服务提供者”关联的Channel上打标记：以发布过的服务
    private static boolean attachPublishEventOnChannel(RegisterMeta meta, Channel channel) {
        // 拿到与“服务提供者”关联的Channel中的自定义属性：AttributeMap，并将本次“服务提供者”发布的服务信息追加进该“服务提供者”Channel的已发布服务的集合中。
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(S_PUBLISH_KEY);
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

    // 取消在channel的标记(发布过的服务)
    private static boolean attachPublishCancelEventOnChannel(RegisterMeta meta, Channel channel) {
        Attribute<ConcurrentSet<RegisterMeta>> attr = channel.attr(S_PUBLISH_KEY);
        ConcurrentSet<RegisterMeta> registerMetaSet = attr.get();
        if (registerMetaSet == null) {
            ConcurrentSet<RegisterMeta> newRegisterMetaSet = new ConcurrentSet<>();
            registerMetaSet = attr.setIfAbsent(newRegisterMetaSet);
            if (registerMetaSet == null) {
                registerMetaSet = newRegisterMetaSet;
            }
        }

        return registerMetaSet.remove(meta);
    }

    // 在与“消费者”关联的Channel打标记：订阅过的服务
    private static boolean attachSubscribeEventOnChannel(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        // 拿到与“消费者”关联的Channel中的自定义属性：AttributeMap，并将本次“消费者”订阅的服务信息追加到该“消费者”Channel的已订阅服务集合中。
        Attribute<ConcurrentSet<RegisterMeta.ServiceMeta>> attr = channel.attr(S_SUBSCRIBE_KEY);
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

    // 检查与“消费者”关联的Channel上的标记：是否订阅过指定的服务
    private static boolean isChannelSubscribeOnServiceMeta(RegisterMeta.ServiceMeta serviceMeta, Channel channel) {
        // 循环遍历与“客户端”关联Channel集合ChannelGroup中对应的“客户端”关联Channel中的订阅服务信息，是否包含“服务提供者”本次发布的服务信息。
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = channel.attr(S_SUBSCRIBE_KEY).get();
        return serviceMetaSet != null && serviceMetaSet.contains(serviceMeta);
    }

    @SuppressWarnings("all")
    private class AckTimeoutScanner implements Runnable {

        @Override
        public void run() {
            for (;;) {
                try {
                    for (RegistryServerMessageNonAck m : messagesNonAck.values()) {
                        if (SystemClock.millisClock().now() - m.timestamp > TimeUnit.SECONDS.toMillis(10)) {

                            // 移除
                            if (messagesNonAck.remove(m.id) == null) {
                                continue;
                            }

                            if (registerInfoContext.getRegisterMeta(m.serviceMeta).getVersion() > m.version) {
                                // 旧版本的内容不需要重发
                                continue;
                            }

                            if (m.channel.isActive()) {
                                RegistryServerMessageNonAck msgNonAck = new RegistryServerMessageNonAck(m.serviceMeta, m.msg, m.channel);
                                messagesNonAck.put(msgNonAck.id, msgNonAck);
                                m.channel.writeAndFlush(m.msg)
                                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                            }
                        }
                    }
                } catch (Throwable t) {
                    logger.error("An exception was caught while scanning the timeout acknowledges {}.", t);
                }

                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    {
        Thread t = new Thread(new AckTimeoutScanner(), "ack.timeout.scanner");
        t.setDaemon(true);
        t.start();
    }

}
