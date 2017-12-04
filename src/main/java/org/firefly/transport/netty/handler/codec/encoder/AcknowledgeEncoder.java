package org.firefly.transport.netty.handler.codec.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.firefly.model.transport.ack.Acknowledge;
import org.firefly.model.transport.protocol.JProtocolHeader;

/**
 * ACK encoder.
 *
 * jupiter
 * org.jupiter.transport.netty.handler
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class AcknowledgeEncoder extends MessageToByteEncoder<Acknowledge> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Acknowledge ack, ByteBuf out) throws Exception {
        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(JProtocolHeader.ACK)
                .writeByte(0)
                .writeLong(ack.sequence())
                .writeInt(0);
    }
}
