package org.firefly.transport.netty.handler.codec.decoder.registryserver;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.firefly.common.util.exception.Signal;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.transport.ack.Acknowledge;
import org.firefly.model.transport.protocol.JProtocolHeader;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import org.firefly.transport.api.exception.IoSignals;

import java.util.List;

public class RegistryServerPublishSubscriptionMessageDecoder extends ReplayingDecoder<RegistryServerPublishSubscriptionMessageDecoder.State> {

    public RegistryServerPublishSubscriptionMessageDecoder() {
        super(State.HEADER_MAGIC);
    }

    // 协议头
    private final JProtocolHeader header = new JProtocolHeader();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case HEADER_MAGIC:
                checkMagic(in.readShort());             // MAGIC
                checkpoint(State.HEADER_SIGN);
            case HEADER_SIGN:
                header.sign(in.readByte());             // 消息标志位
                checkpoint(State.HEADER_STATUS);
            case HEADER_STATUS:
                in.readByte();                          // no-op
                checkpoint(State.HEADER_ID);
            case HEADER_ID:
                header.id(in.readLong());               // 消息id
                checkpoint(State.HEADER_BODY_LENGTH);
            case HEADER_BODY_LENGTH:
                header.bodyLength(in.readInt());        // 消息体长度
                checkpoint(State.BODY);
            case BODY:
                byte s_code = header.serializerCode();

                switch (header.messageCode()) {
                    case JProtocolHeader.HEARTBEAT:
                        break;
                    case JProtocolHeader.PUBLISH_SERVICE:
                    case JProtocolHeader.PUBLISH_CANCEL_SERVICE:
                    case JProtocolHeader.SUBSCRIBE_SERVICE:
                    case JProtocolHeader.OFFLINE_NOTICE: {
                        byte[] bytes = new byte[header.bodyLength()];
                        in.readBytes(bytes);

                        Serializer serializer = SerializerFactory.getSerializer(s_code);
                        PublishSubscriptionMessage msg = serializer.readObject(bytes, PublishSubscriptionMessage.class);
                        msg.messageCode(header.messageCode());
                        out.add(msg);

                        break;
                    }
                    case JProtocolHeader.ACK:
                        out.add(new Acknowledge(header.id()));

                        break;
                    default:
                        throw IoSignals.ILLEGAL_SIGN;
                }
                checkpoint(State.HEADER_MAGIC);
        }
    }

    private static void checkMagic(short magic) throws Signal {
        if (magic != JProtocolHeader.MAGIC) {
            throw IoSignals.ILLEGAL_MAGIC;
        }
    }

    enum State {
        HEADER_MAGIC,
        HEADER_SIGN,
        HEADER_STATUS,
        HEADER_ID,
        HEADER_BODY_LENGTH,
        BODY
    }
}
