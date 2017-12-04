package org.firefly.transport.netty.connector.connection.consumer;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.transport.api.connector.connection.JConnection;
import org.firefly.transport.netty.handler.connector.ConnectionWatchdog;

public class ConsumerToProviderNettyConnection extends JConnection {

    private final ChannelFuture future;
    private ConnectionWatchdog watchdog;

    public ConsumerToProviderNettyConnection(UnresolvedAddress address, ChannelFuture future, ConnectionWatchdog watchdog) {
        super(address);
        this.future = future;
        this.watchdog = watchdog;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    @Override
    public void operationComplete(final Runnable callback) {
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    callback.run();
                }
            }
        });
    }

    // 在 JConnectionManager 中调用
    @Override
    public void setReconnect(boolean reconnect) {
        if (reconnect) {
            watchdog.start();
        } else {
            watchdog.stop();
        }
    }
}
