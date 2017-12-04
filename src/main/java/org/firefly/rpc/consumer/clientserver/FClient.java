package org.firefly.rpc.consumer.clientserver;

import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.registry.api.RegistryService;
import org.firefly.registry.api.consumer.NotifyListener;
import org.firefly.registry.api.consumer.OfflineListener;
import org.firefly.transport.api.connector.JConnector;
import org.firefly.transport.api.connector.connection.JConnection;
import java.util.Collection;

/**
 * 注意 FClient 单例即可, 不要创建多个实例.
 */
public interface FClient {

    /**
     * 每一个应用都建议设置一个appName.
     */
    String appName();

    /**
     * 网络层connector.
     */
    JConnector<JConnection> connector();

    /**
     * 设置网络层connector.
     */
    FClient withConnector(JConnector<JConnection> connector);

    /**
     * 注册服务实例：是个动作。
     */
    RegistryService getRegistryService();

    /**
     * 从本地容器查找服务信息.
     */
    Collection<RegisterMeta> lookup(Directory directory);

    /**
     * 设置对指定服务由jupiter自动管理连接.
     */
    JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass);

    /**
     * 设置对指定服务由jupiter自动管理连接.
     */
    JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass, String version);

    /**
     * 设置对指定服务由jupiter自动管理连接.
     */
    JConnector.ConnectionWatcher watchConnections(Directory directory);

    /**
     * 阻塞等待一直到该服务有可用连接或者超时.
     */
    boolean awaitConnections(Directory directory, long timeoutMillis);

    /**
     * 从注册中心订阅一个服务.
     */
    void subscribe(Directory directory, NotifyListener listener);

    /**
     * 服务下线通知.
     */
    void offlineListening(UnresolvedAddress address, OfflineListener listener);

    /**
     * 优雅关闭jupiter client.
     */
    void shutdownGracefully();


    JConnector<JConnection> getConnector();

}
