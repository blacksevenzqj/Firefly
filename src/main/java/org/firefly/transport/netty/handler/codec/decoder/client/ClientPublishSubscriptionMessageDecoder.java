package org.firefly.transport.netty.handler.codec.decoder.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.firefly.common.util.Signal;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.transport.ack.Acknowledge;
import org.firefly.model.transport.protocol.JProtocolHeader;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import org.firefly.transport.api.exception.IoSignals;

import java.util.List;

/**
 * <pre>
 * **************************************************************************************************
 *                                          Protocol
 *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *       2   │   1   │    1   │     8     │      4      │
 *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
 *           │       │        │           │             │
 *  │  MAGIC   Sign    Status   Invoke Id   Body Length                   Body Content              │
 *           │       │        │           │             │
 *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *
 * 消息头16个字节定长
 * = 2 // magic = (short) 0xbabe
 * + 1 // 消息标志位, 低地址4位用来表示消息类型, 高地址4位用来表示序列化类型
 * + 1 // 空
 * + 8 // 消息 id, long 类型
 * + 4 // 消息体 body 长度, int 类型
 * </pre>
 */
public class ClientPublishSubscriptionMessageDecoder extends ReplayingDecoder<ClientPublishSubscriptionMessageDecoder.State> {

    public ClientPublishSubscriptionMessageDecoder() {
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
                    case JProtocolHeader.PUBLISH_SERVICE:
                    case JProtocolHeader.PUBLISH_CANCEL_SERVICE:
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
