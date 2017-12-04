package org.firefly.rpc.provider.server;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.concurrent.thread.NamedThreadFactory;
import org.firefly.common.util.ClassUtil;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.spi.JServiceLoader;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.rpc.metadata.ServiceMetadata;
import org.firefly.model.rpc.metadata.ServiceWrapper;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.registry.api.RegistryService;
import org.firefly.rpc.provider.processor.DefaultProviderProcessor;
import org.firefly.rpc.provider.server.servicecontainer.DefaultServiceProviderContainer;
import org.firefly.rpc.provider.server.servicecontainer.ServiceProviderContainer;
import org.firefly.rpc.provider.server.servicewrapperfactory.DefaultServiceWrapperProducer;
import org.firefly.rpc.provider.server.servicewrapperfactory.ServiceWrapperProducer;
import org.firefly.transport.api.acceptor.JAcceptor;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import static org.firefly.common.util.exception.StackTraceUtil.stackTrace;

public class DefaultProviderServer implements JServer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultProviderServer.class);

    static {
        // touch off TracingUtil.<clinit>
        // because getLocalAddress() and getPid() sometimes too slow
        ClassUtil.classInitialize("org.firefly.rpc.tracking.TracingUtil", 500);
    }

    // 服务延迟初始化的默认线程池
    private final Executor defaultInitializerExecutor =
            Executors.newSingleThreadExecutor(new NamedThreadFactory("initializer"));

    // provider本地容器
    private final ServiceProviderContainer providerContainer = new DefaultServiceProviderContainer();

    // 服务发布(SPI)
    private final RegistryService registryService;

    // IO acceptor
    private JAcceptor acceptor;

    public DefaultProviderServer() {
        this(RegistryService.RegistryType.DEFAULT);
    }

    /**
     * @param registryType
     * 根据RegistryType参数，在RegistryService接口实现类上查找SpiImpl注解，SpiImpl注解值匹配RegistryType参数的RegistryService接口实现类。
     * 1、DefaultRegistryService
     * 2、ZookeeperRegistryService
     */
    public DefaultProviderServer(RegistryService.RegistryType registryType) {
        registryType = registryType == null ? RegistryService.RegistryType.DEFAULT : registryType;
        registryService = JServiceLoader.load(RegistryService.class).find(registryType.getValue());
    }

    @Override
    public JAcceptor acceptor() {
        return acceptor;
    }

    /**
     * @param acceptor
     * 将本类DefaultServer对象“服务”作为参数构建服务提供者处理实例，并将服务提供者处理实例传入传输层创建传输层实例。
     */
    @Override
    public JServer withAcceptor(JAcceptor acceptor) {
        acceptor.withProcessor(new DefaultProviderProcessor(this));
        this.acceptor = acceptor;
        return this;
    }

    @Override
    public RegistryService getRegistryService() {
        return registryService;
    }

    @Override
    public ServiceWrapperProducer serviceWrapperProducer() {
        return new DefaultServiceWrapperProducer(providerContainer);
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return providerContainer.lookupService(directory.directory());
    }

    @Override
    public ServiceWrapper removeService(Directory directory) {
        return providerContainer.removeService(directory.directory());
    }

    @Override
    public List<ServiceWrapper> allRegisteredServices() {
        return providerContainer.getAllServices();
    }

    @Override
    public void publish(ServiceWrapper serviceWrapper) {
        ServiceMetadata metadata = serviceWrapper.getMetadata();

        RegisterMeta meta = new RegisterMeta();
        meta.setPort(acceptor.boundPort());
        meta.setGroup(metadata.getGroup());
        meta.setServiceProviderName(metadata.getServiceProviderName());
        meta.setVersion(metadata.getVersion());
        meta.setWeight(serviceWrapper.getWeight());
//        meta.setConnCount(JConstants.SUGGESTED_CONNECTION_COUNT);
        meta.setConnCount(1);

        registryService.register(meta);
    }

    @Override
    public void publish(ServiceWrapper... serviceWrappers) {
        for (ServiceWrapper wrapper : serviceWrappers) {
            publish(wrapper);
        }
    }

    @Override
    public <T> void publishWithInitializer(ServiceWrapper serviceWrapper, ProviderInitializer<T> initializer) {
        publishWithInitializer(serviceWrapper, initializer, null);
    }

    @Override
    public <T> void publishWithInitializer(
            final ServiceWrapper serviceWrapper, final ProviderInitializer<T> initializer, Executor executor) {
        Runnable task = new Runnable() {

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    initializer.init((T) serviceWrapper.getServiceProvider());
                    publish(serviceWrapper);
                } catch (Exception e) {
                    logger.error("Error on {} #publishWithInitializer: {}.", serviceWrapper.getMetadata(), stackTrace(e));
                }
            }
        };
        if (executor == null) {
            defaultInitializerExecutor.execute(task);
        } else {
            executor.execute(task);
        }
    }

    @Override
    public void publishAll() {
        for (ServiceWrapper wrapper : providerContainer.getAllServices()) {
            publish(wrapper);
        }
    }

    @Override
    public void unpublish(ServiceWrapper serviceWrapper) {
        ServiceMetadata metadata = serviceWrapper.getMetadata();

        RegisterMeta meta = new RegisterMeta();
        meta.setPort(acceptor.boundPort());
        meta.setGroup(metadata.getGroup());
        meta.setVersion(metadata.getVersion());
        meta.setServiceProviderName(metadata.getServiceProviderName());
        meta.setWeight(serviceWrapper.getWeight());
        meta.setConnCount(JConstants.SUGGESTED_CONNECTION_COUNT);

        registryService.unregister(meta);
    }

    @Override
    public void unpublishAll() {
        for (ServiceWrapper wrapper : providerContainer.getAllServices()) {
            unpublish(wrapper);
        }
    }

    @Override
    public void start() throws InterruptedException {
        acceptor.start();
    }

    @Override
    public void start(boolean sync) throws InterruptedException {
        acceptor.start(sync);
    }

    @Override
    public void shutdownGracefully() {
        registryService.shutdownGracefully();
        acceptor.shutdownGracefully();
    }

    public void setAcceptor(JAcceptor acceptor) {
        withAcceptor(acceptor);
    }
}
