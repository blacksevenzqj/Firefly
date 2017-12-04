package org.firefly.transport.netty.handler.codec.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.firefly.model.registry.PublishSubscriptionMessage;
import org.firefly.model.transport.protocol.JProtocolHeader;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;


@ChannelHandler.Sharable
public class PublishSubscriptionMessageEncoder extends MessageToByteEncoder<PublishSubscriptionMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, PublishSubscriptionMessage msg, ByteBuf out) throws Exception {
        byte s_code = msg.serializerCode();
        byte sign = JProtocolHeader.toSign(s_code, msg.messageCode());
        Serializer serializer = SerializerFactory.getSerializer(s_code);
        byte[] bytes = serializer.writeObject(msg);

        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(sign)
                .writeByte(0)
                .writeLong(0)
                .writeInt(bytes.length)
                .writeBytes(bytes);
    }
}
