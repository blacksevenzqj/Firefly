package org.firefly.transport.netty.handler.connector;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.firefly.model.transport.protocol.JProtocolHeader;

public class Heartbeats {

    private static final ByteBuf HEARTBEAT_BUF;

    static {
        // 分配堆缓冲区
        ByteBuf buf = Unpooled.buffer(JProtocolHeader.HEAD_LENGTH);
        buf.writeShort(JProtocolHeader.MAGIC);
        buf.writeByte(JProtocolHeader.HEARTBEAT); // 心跳包这里可忽略高地址的4位序列化/反序列化标志
        buf.writeByte(0);
        buf.writeLong(0);
        buf.writeInt(0);
        HEARTBEAT_BUF = Unpooled.unreleasableBuffer(buf).asReadOnly();
    }

    /**
     * Returns the shared heartbeat content.
     */
    public static ByteBuf heartbeatContent() {
        return HEARTBEAT_BUF.duplicate();
    }
}
