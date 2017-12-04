package org.firefly.transport.netty.handler.connector;

import io.netty.channel.ChannelHandler;

public interface ChannelHandlerHolder {

    ChannelHandler[] handlers();
}
