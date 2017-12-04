package org.firefly.rpc.provider.server;

import org.firefly.model.rpc.metadata.ServiceWrapper;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.registry.api.RegistryService;
import org.firefly.registry.api.provider.Registry;
import org.firefly.rpc.provider.server.servicewrapperfactory.ServiceWrapperProducer;
import org.firefly.transport.api.acceptor.JAcceptor;
import java.util.List;
import java.util.concurrent.Executor;

public interface JServer {

    interface ProviderInitializer<T> {
        /**
         * 初始化指定服务提供者.
         */
        void init(T provider);
    }

    /**
     * 网络层acceptor.
     */
    JAcceptor acceptor();

    /**
     * 设置网络层acceptor.
     */
    JServer withAcceptor(JAcceptor acceptor);

    /**
     * 注册服务实例：是个动作。
     */
    RegistryService getRegistryService();

    /**
     * 获取服务注册(本地)工具.
     */
    ServiceWrapperProducer serviceWrapperProducer();

    /**
     * 根据服务目录查找对应服务提供者.
     */
    ServiceWrapper lookupService(Directory directory);

    /**
     * 根据服务目录移除对应服务提供者.
     */
    ServiceWrapper removeService(Directory directory);

    /**
     * 注册所有服务到本地容器.
     */
    List<ServiceWrapper> allRegisteredServices();

    /**
     * 发布指定服务到注册中心.
     */
    void publish(ServiceWrapper serviceWrapper);

    /**
     * 发布指定服务列表到注册中心.
     */
    void publish(ServiceWrapper... serviceWrappers);

    /**
     * 服务提供者初始化完成后再发布服务到注册中心(延迟发布服务).
     */
    <T> void publishWithInitializer(ServiceWrapper serviceWrapper, ProviderInitializer<T> initializer);

    /**
     * 服务提供者初始化完成后再发布服务到注册中心(延迟发布服务), 并设置服务私有的线程池来执行初始化操作.
     */
    <T> void publishWithInitializer(ServiceWrapper serviceWrapper, ProviderInitializer<T> initializer, Executor executor);

    /**
     * 发布本地所有服务到注册中心.
     */
    void publishAll();

    /**
     * 从注册中心把指定服务下线.
     */
    void unpublish(ServiceWrapper serviceWrapper);

    /**
     * 从注册中心把本地所有服务全部下线.
     */
    void unpublishAll();

    /**
     * 启动jupiter server, 以同步阻塞的方式启动.
     */
    void start() throws InterruptedException;

    /**
     * 启动jupiter server, 可通过参数指定异步/同步的方式启动.
     */
    void start(boolean sync) throws InterruptedException;

    /**
     * 优雅关闭jupiter server.
     */
    void shutdownGracefully();
}
