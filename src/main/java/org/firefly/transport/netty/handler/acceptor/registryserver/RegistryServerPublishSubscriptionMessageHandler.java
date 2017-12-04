package org.firefly.transport.netty.handler.acceptor.registryserver;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ConcurrentSet;
import org.firefly.common.util.exception.Signal;
import org.firefly.common.util.Strings;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.transport.ack.Acknowledge;
import org.firefly.model.transport.protocol.JProtocolHeader;
import org.firefly.transport.netty.acceptor.DefaultRegistryServerNettyTcpAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

@ChannelHandler.Sharable
public class RegistryServerPublishSubscriptionMessageHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServerPublishSubscriptionMessageHandler.class);

    private DefaultRegistryServerNettyTcpAcceptor registryServer;

    public RegistryServerPublishSubscriptionMessageHandler(DefaultRegistryServerNettyTcpAcceptor registryServer) {
        this.registryServer = registryServer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("DefaultRegistryServer MessageHandler channelActive 了");
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (msg instanceof PublishSubscriptionMessage) {
            PublishSubscriptionMessage obj = (PublishSubscriptionMessage) msg;

            switch (obj.messageCode()) {
                case JProtocolHeader.PUBLISH_SERVICE:
                case JProtocolHeader.PUBLISH_CANCEL_SERVICE:   // 处理 “服务提供者” 发布服务 或 取消发布服务。 Channel为 “服务提供者”
                    RegisterMeta meta = (RegisterMeta) obj.data();
                    if (Strings.isNullOrEmpty(meta.getHost())) {
                        SocketAddress address = ch.remoteAddress();
                        if (address instanceof InetSocketAddress) {
                            meta.setHost(((InetSocketAddress) address).getAddress().getHostAddress());
                        } else {
                            logger.warn("Could not get remote host: {}, info: {}", ch, meta);
                            return;
                        }
                    }

                    if (obj.messageCode() == JProtocolHeader.PUBLISH_SERVICE) {
                        // 请求流程："服务提供者" -> “注册服务” -> “客户端”
                        // 面向 订阅了“服务提供者”本次发布服务 的所有"客户端"，发送"服务提供者"的服务消息给“消费者”。
                        registryServer.handlePublish(meta, ch);
                    } else if (obj.messageCode() == JProtocolHeader.PUBLISH_CANCEL_SERVICE) {
                        registryServer.handlePublishCancel(meta, ch);
                    }
                    // 面向 发布服务的“服务提供者”，发送回复ACK消息给“服务提供者”。
                    ch.writeAndFlush(new Acknowledge(obj.sequence())) // 回复ACK
                            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                    break;
                case JProtocolHeader.SUBSCRIBE_SERVICE: // 处理 “消费者” 订阅服务。Channel为 “消费者”
                    // 请求流程："消费者" -> “注册服务” -> “消费者”
                    // 面向 订阅指定服务的"客户端"，发送"服务提供者"的服务消息给“消费者”。
                    registryServer.handleSubscribe((RegisterMeta.ServiceMeta) obj.data(), ch);
                    // 面向 订阅服务的“消费者”，发送回复ACK消息给“消费者”。
                    ch.writeAndFlush(new Acknowledge(obj.sequence())) // 回复ACK
                            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                    break;
                case JProtocolHeader.OFFLINE_NOTICE:
                    registryServer.handleOfflineNotice((RegisterMeta.Address) obj.data());

                    break;
            }
        } else if (msg instanceof Acknowledge) {
            registryServer.handleAcknowledge((Acknowledge) msg, ch);
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);
            }

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();

        // 取消之前发布的所有服务
        ConcurrentSet<RegisterMeta> registerMetaSet = ch.attr(DefaultRegistryServerNettyTcpAcceptor.S_PUBLISH_KEY).get();

        if (registerMetaSet == null || registerMetaSet.isEmpty()) {
            return;
        }

        RegisterMeta.Address address = null;
        for (RegisterMeta meta : registerMetaSet) {
            if (address == null) {
                address = meta.getAddress();
            }
            registryServer.handlePublishCancel(meta, ch);
        }

        if (address != null) {
            // 通知所有订阅者对应机器下线
            registryServer.handleOfflineNotice(address);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();
        ChannelConfig config = ch.config();

        // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
        // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
        if (!ch.isWritable()) {
            // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
            if (logger.isWarnEnabled()) {
                logger.warn("{} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(false);
        } else {
            // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
            if (logger.isWarnEnabled()) {
                logger.warn("{} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel ch = ctx.channel();

        if (cause instanceof Signal) {
            logger.error("An I/O signal was caught: {}, force to close channel: {}.", ((Signal) cause).name(), ch);

            ch.close();
        } else if (cause instanceof IOException) {
            logger.error("An I/O exception was caught: {}, force to close channel: {}.", cause, cause);

            ch.close();
        } else {
            logger.error("An unexpected exception was caught: {}, channel: {}.", cause, ch);
        }
    }


}
