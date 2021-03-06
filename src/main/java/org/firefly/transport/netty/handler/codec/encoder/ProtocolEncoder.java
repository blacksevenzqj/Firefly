package org.firefly.transport.netty.handler.codec.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.firefly.common.util.Reflects;
import org.firefly.model.rpc.BytesHolder;
import org.firefly.model.rpc.request.JRequestBytes;
import org.firefly.model.rpc.response.JResponseBytes;
import org.firefly.model.transport.protocol.JProtocolHeader;

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
 * + 1 // 消息标志位, 低地址4位用来表示消息类型request/response/heartbeat等, 高地址4位用来表示序列化类型
 * + 1 // 状态位, 设置请求响应状态
 * + 8 // 消息 id, long 类型, 未来jupiter可能将id限制在48位, 留出高地址的16位作为扩展字段
 * + 4 // 消息体 body 长度, int 类型
 * </pre>
 */

@ChannelHandler.Sharable
public class ProtocolEncoder extends MessageToByteEncoder<BytesHolder> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BytesHolder msg, ByteBuf out) throws Exception {
        if (msg instanceof JRequestBytes) {
            doEncodeRequest((JRequestBytes) msg, out);
        } else if (msg instanceof JResponseBytes) {
            doEncodeResponse((JResponseBytes) msg, out);
        } else {
            throw new IllegalArgumentException(Reflects.simpleClassName(msg));
        }
    }

    private void doEncodeRequest(JRequestBytes request, ByteBuf out) {
        // request.serializerCode() = (byte) 0x01, JProtocolHeader.REQUEST = 0x01
        byte sign = JProtocolHeader.toSign(request.serializerCode(), JProtocolHeader.REQUEST);
        long invokeId = request.invokeId();
        byte[] bytes = request.bytes();
        int length = bytes.length;

        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(sign)
                .writeByte(0x00)
                .writeLong(invokeId)
                .writeInt(length)
                .writeBytes(bytes);
    }

    private void doEncodeResponse(JResponseBytes response, ByteBuf out) {
        byte sign = JProtocolHeader.toSign(response.serializerCode(), JProtocolHeader.RESPONSE);
        byte status = response.status();
        long invokeId = response.id();
        byte[] bytes = response.bytes();
        int length = bytes.length;

        out.writeShort(JProtocolHeader.MAGIC)
                .writeByte(sign)
                .writeByte(status)
                .writeLong(invokeId)
                .writeInt(length)
                .writeBytes(bytes);
    }
}
