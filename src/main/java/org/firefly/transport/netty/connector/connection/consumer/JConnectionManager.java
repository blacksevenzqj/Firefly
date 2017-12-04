package org.firefly.transport.netty.connector.connection.consumer;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.transport.api.connector.connection.JConnection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消费者 连接 服务提供者 连接管理
 * 连接管理器, 用于自动管理(按照地址归组)连接。
 */
public class JConnectionManager {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(JConnectionManager.class);

    private final ConcurrentMap<UnresolvedAddress, CopyOnWriteArrayList<JConnection>> connections = Maps.newConcurrentMap();

    /**
     * 设置为自动管理连接
     */
    public void manage(JConnection connection) {
        UnresolvedAddress address = connection.getAddress();
        CopyOnWriteArrayList<JConnection> list = connections.get(address);
        if (list == null) {
            CopyOnWriteArrayList<JConnection> newList = new CopyOnWriteArrayList<>();
            list = connections.putIfAbsent(address, newList);
            if (list == null) {
                list = newList;
            }
        }
        list.add(connection);
    }

    /**
     * 取消对指定地址的自动重连
     */
    public void cancelReconnect(UnresolvedAddress address) {
        CopyOnWriteArrayList<JConnection> list = connections.remove(address);
        if (list != null) {
            for (JConnection c : list) {
                c.setReconnect(false);
            }
            logger.warn("Cancel reconnect to: {}.", address);
        }
    }

    /**
     * 取消对所有地址的自动重连
     */
    public void cancelAllReconnect() {
        for (UnresolvedAddress address : connections.keySet()) {
            cancelReconnect(address);
        }
    }
}
