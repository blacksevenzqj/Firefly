package org.firefly.rpc.consumer.clientserver;

import org.firefly.common.util.ClassUtil;
import org.firefly.common.util.Strings;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.spi.JServiceLoader;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.rpc.metadata.ServiceMetadata;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.registry.api.AbstractRegistryService;
import org.firefly.registry.api.RegistryService;
import org.firefly.registry.api.consumer.NotifyListener;
import org.firefly.registry.api.consumer.OfflineListener;
import org.firefly.rpc.consumer.processor.DefaultConsumerProcessor;
import org.firefly.rpc.provider.annotation.ServiceProvider;
import org.firefly.transport.api.connector.JConnector;
import org.firefly.transport.api.connector.connection.JConnection;
import org.firefly.transport.netty.connector.connection.consumer.ConsumerToProviderConnectionWatcher;

import java.util.Collection;

import static org.firefly.common.util.Preconditions.checkNotNull;

public class DefaultClient implements FClient {

    static {
        // touch off TracingUtil.<clinit>
        // because getLocalAddress() and getPid() sometimes too slow
        ClassUtil.classInitialize("org.firefly.rpc.tracking.TracingUtil", 500);
    }

    // 服务订阅(SPI)
    private final RegistryService registryService;

    private final String appName;

    private JConnector<JConnection> connector;

    public DefaultClient() {
        this(JConstants.UNKNOWN_APP_NAME, RegistryService.RegistryType.DEFAULT);
    }

    public DefaultClient(RegistryService.RegistryType registryType) {
        this(JConstants.UNKNOWN_APP_NAME, registryType);
    }

    public DefaultClient(String appName) {
        this(appName, RegistryService.RegistryType.DEFAULT);
    }

    public DefaultClient(String appName, RegistryService.RegistryType registryType) {
        this.appName = Strings.isBlank(appName) ? JConstants.UNKNOWN_APP_NAME : appName;  // UNKNOWN
        registryType = registryType == null ? RegistryService.RegistryType.DEFAULT : registryType;
        registryService = JServiceLoader.load(RegistryService.class).find(registryType.getValue());
    }

    @Override
    public String appName() {
        return appName;  // UNKNOWN
    }

    @Override
    public JConnector<JConnection> connector() {
        return connector;
    }

    @Override
    public FClient withConnector(JConnector<JConnection> connector) {
        connector.withProcessor(new DefaultConsumerProcessor());
        this.connector = connector;
        return this;
    }

    @Override
    public RegistryService getRegistryService() {
        return registryService;
    }

    @Override
    public Collection<RegisterMeta> lookup(Directory directory) {
        RegisterMeta.ServiceMeta serviceMeta = toServiceMeta(directory);
        return registryService.lookup(serviceMeta);
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass) {
        return watchConnections(interfaceClass, JConstants.DEFAULT_VERSION);
    }

    @Override
    public JConnector.ConnectionWatcher watchConnections(Class<?> interfaceClass, String version) {
        checkNotNull(interfaceClass, "interfaceClass");
        ServiceProvider annotation = interfaceClass.getAnnotation(ServiceProvider.class);
        checkNotNull(annotation, interfaceClass + " is not a ServiceProvider interface");
        String providerName = annotation.name();
        providerName = Strings.isNotBlank(providerName) ? providerName : interfaceClass.getName();
        version = Strings.isNotBlank(version) ? version : JConstants.DEFAULT_VERSION;

        return watchConnections(new ServiceMetadata(annotation.group(), providerName, version));
    }

    // 订阅服务：是根据目标接口上的 ServiceProvider注解 的属性值 来作为参数。
    @Override
    public JConnector.ConnectionWatcher watchConnections(final Directory directory) {
        JConnector.ConnectionWatcher manager = new ConsumerToProviderConnectionWatcher(this, directory);
        manager.start();
        return manager;
    }

    @Override
    public boolean awaitConnections(Directory directory, long timeoutMillis) {
        JConnector.ConnectionWatcher watcher = watchConnections(directory);
        return watcher.waitForAvailable(timeoutMillis);
    }

    @Override
    public void subscribe(Directory directory, NotifyListener listener) {
        registryService.subscribe(toServiceMeta(directory), listener);
    }

    @Override
    public void offlineListening(UnresolvedAddress address, OfflineListener listener) {
        if (registryService instanceof AbstractRegistryService) {
            ((AbstractRegistryService) registryService).offlineListening(toAddress(address), listener);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void shutdownGracefully() {
        connector.shutdownGracefully();
    }


    // setter for spring-support
    public void setConnector(JConnector<JConnection> connector) {
        withConnector(connector);
    }

    private static RegisterMeta.ServiceMeta toServiceMeta(Directory directory) {
        RegisterMeta.ServiceMeta serviceMeta = new RegisterMeta.ServiceMeta();
        serviceMeta.setGroup(checkNotNull(directory.getGroup(), "group"));
        serviceMeta.setServiceProviderName(checkNotNull(directory.getServiceProviderName(), "serviceProviderName"));
        serviceMeta.setVersion(checkNotNull(directory.getVersion(), "version"));
        return serviceMeta;
    }

    private static RegisterMeta.Address toAddress(UnresolvedAddress address) {
        return new RegisterMeta.Address(address.getHost(), address.getPort());
    }

    @Override
    public JConnector<JConnection> getConnector() {
        return connector;
    }
}
