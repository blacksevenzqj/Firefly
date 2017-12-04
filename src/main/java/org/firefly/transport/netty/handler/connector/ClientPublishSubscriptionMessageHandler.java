package org.firefly.transport.netty.handler.connector;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.Pair;
import org.firefly.common.util.exception.Signal;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.registry.nonack.ClientNettyConnectorMessageNonAck;
import org.firefly.model.transport.ack.Acknowledge;
import org.firefly.model.transport.protocol.JProtocolHeader;
import org.firefly.registry.api.consumer.NotifyListener;
import org.firefly.transport.netty.connector.ClientToRegistryNettyConnector;

import java.io.IOException;
import java.util.List;
import static org.firefly.common.util.exception.StackTraceUtil.stackTrace;

@ChannelHandler.Sharable
public class ClientPublishSubscriptionMessageHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClientPublishSubscriptionMessageHandler.class);

    private ClientToRegistryNettyConnector clientNettyConnector;

    public ClientPublishSubscriptionMessageHandler(ClientToRegistryNettyConnector clientNettyConnector) {
        this.clientNettyConnector = clientNettyConnector;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        System.out.println("DefaultRegistry MessageHandler channelActive 了！！！");

        Channel ch = clientNettyConnector.setAndGetClientNettyConnectorChannel(ctx.channel());

        // 重新订阅
        for (RegisterMeta.ServiceMeta serviceMeta : clientNettyConnector.getRegistryService().getSubscribeSet()) {
            // 与doSubscribe()中的write有竞争
            if (!clientNettyConnector.attachSubscribeEventOnChannel(serviceMeta, ch)) {
                continue;
            }

            PublishSubscriptionMessage msg = new PublishSubscriptionMessage(clientNettyConnector.getSerializerType().value());
            msg.messageCode(JProtocolHeader.SUBSCRIBE_SERVICE);
            msg.data(serviceMeta);

            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            ClientNettyConnectorMessageNonAck msgNonAck = new ClientNettyConnectorMessageNonAck(msg);
            clientNettyConnector.getMessagesNonAck().put(msgNonAck.id, msgNonAck);
        }

        // 重新发布服务
        for (RegisterMeta meta : clientNettyConnector.getRegistryService().getRegisterMetaMap().keySet()) {

            System.out.println("DefaultRegistry 中的channelActive中的registryService.getRegisterMetaMap().keySet() --- Start");

            // 与doRegister()中的write有竞争
            if (!clientNettyConnector.attachPublishEventOnChannel(meta, ch)) {
                System.out.println("DefaultRegistry 中的channelActive中的registryService.getRegisterMetaMap().keySet() --- continue掉了");
                continue;
            }

            System.out.println("DefaultRegistry 中的channelActive中的registryService.getRegisterMetaMap().keySet() --- Running");

            PublishSubscriptionMessage msg = new PublishSubscriptionMessage(clientNettyConnector.getSerializerType().value());
            msg.messageCode(JProtocolHeader.PUBLISH_SERVICE);
            msg.data(meta);

            ch.writeAndFlush(msg)
                    .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            ClientNettyConnectorMessageNonAck msgNonAck = new ClientNettyConnectorMessageNonAck(msg);
            clientNettyConnector.getMessagesNonAck().put(msgNonAck.id, msgNonAck);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (msg instanceof PublishSubscriptionMessage) {
            PublishSubscriptionMessage obj = (PublishSubscriptionMessage) msg;

            switch (obj.messageCode()) {
                case JProtocolHeader.PUBLISH_SERVICE: {
                    Pair<RegisterMeta.ServiceMeta, ?> data = (Pair<RegisterMeta.ServiceMeta, ?>) obj.data();
                    Object metaObj = data.getSecond();

                    if (metaObj instanceof List) {
                        List<RegisterMeta> list = (List<RegisterMeta>) metaObj;
                        RegisterMeta[] array = new RegisterMeta[list.size()];
                        list.toArray(array);
                        clientNettyConnector.getRegistryService().notify(
                                data.getFirst(),
                                NotifyListener.NotifyEvent.CHILD_ADDED,
                                obj.version(),
                                array
                        );
                    } else if (metaObj instanceof RegisterMeta) {
                        clientNettyConnector.getRegistryService().notify(
                                data.getFirst(),
                                NotifyListener.NotifyEvent.CHILD_ADDED,
                                obj.version(),
                                (RegisterMeta) metaObj
                        );
                    }

                    ch.writeAndFlush(new Acknowledge(obj.sequence()))  // 回复ACK
                            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                    if (logger.isInfoEnabled()) {
                        logger.info("Publish from RegistryServer {}, metadata: {}, version: {}.",
                                data.getFirst(), metaObj, obj.version());
                    }

                    break;
                }
                case JProtocolHeader.PUBLISH_CANCEL_SERVICE: {

                    System.out.println("DefaultRegistry 取消服务：JProtocolHeader.PUBLISH_CANCEL_SERVICE");

                    Pair<RegisterMeta.ServiceMeta, RegisterMeta> data =
                            (Pair<RegisterMeta.ServiceMeta, RegisterMeta>) obj.data();
                    clientNettyConnector.getRegistryService().notify(
                            data.getFirst(), NotifyListener.NotifyEvent.CHILD_REMOVED, obj.version(), data.getSecond());

                    ch.writeAndFlush(new Acknowledge(obj.sequence()))  // 回复ACK
                            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

                    if (logger.isInfoEnabled()) {
                        logger.info("Publish cancel from RegistryServer {}, metadata: {}, version: {}.",
                                data.getFirst(), data.getSecond(), obj.version());
                    }

                    break;
                }
                case JProtocolHeader.OFFLINE_NOTICE:
                    RegisterMeta.Address address = (RegisterMeta.Address) obj.data();

                    logger.info("Offline notice on {}.", address);

                    clientNettyConnector.getRegistryService().offline(address);

                    break;
            }
        } else if (msg instanceof Acknowledge) {
            clientNettyConnector.handleAcknowledge((Acknowledge) msg);
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);
            }

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel ch = ctx.channel();

        if (cause instanceof Signal) {
            logger.error("An I/O signal was caught: {}, force to close channel: {}.", ((Signal) cause).name(), ch);

            ch.close();
        } else if (cause instanceof IOException) {
            logger.error("An I/O exception was caught: {}, force to close channel: {}.", stackTrace(cause), ch);

            ch.close();
        } else {
            logger.error("An unexpected exception was caught: {}, channel: {}.", stackTrace(cause), ch);
        }
    }
}
