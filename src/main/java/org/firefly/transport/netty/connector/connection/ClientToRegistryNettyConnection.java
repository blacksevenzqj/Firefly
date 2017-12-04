package org.firefly.transport.netty.connector.connection;

import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.transport.api.connector.connection.JConnection;
import org.firefly.transport.netty.handler.connector.ConnectionWatchdog;

public class ClientToRegistryNettyConnection extends JConnection {

    private ConnectionWatchdog watchdog;

    public ClientToRegistryNettyConnection(UnresolvedAddress address, ConnectionWatchdog watchdog) {
        super(address);
        this.watchdog = watchdog;
    }

    // 服务提供者、消费者 连接 注册服务 后返回的 JConnection实例。
    // DefaultRegistryService 的 connectToRegistryServer方法 没有对 ClientToRegistryNettyConnector的connect方法返回的JConnection实例做处理。
    @Override
    public void setReconnect(boolean reconnect) {
        if (reconnect) {
            watchdog.start();
        } else {
            watchdog.stop();
        }
    }
}
