package org.firefly.rpc.provider.server.servicecontainer;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.internal.Lists;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.rpc.metadata.ServiceWrapper;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

// 本地provider容器默认实现
public final class DefaultServiceProviderContainer implements ServiceProviderContainer {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultServiceProviderContainer.class);

    // Key为ServiceMetadata.directory()
    private final ConcurrentMap<String, ServiceWrapper> serviceProviders = Maps.newConcurrentMap();

    @Override
    public void registerService(String uniqueKey, ServiceWrapper serviceWrapper) {
        serviceProviders.put(uniqueKey, serviceWrapper);

        if (logger.isDebugEnabled()) {
            logger.debug("ServiceProvider [{}, {}] is registered.", uniqueKey, serviceWrapper.getServiceProvider());
        }
    }

    @Override
    public ServiceWrapper lookupService(String uniqueKey) {
        return serviceProviders.get(uniqueKey);
    }

    @Override
    public ServiceWrapper removeService(String uniqueKey) {
        ServiceWrapper provider = serviceProviders.remove(uniqueKey);
        if (provider == null) {
            logger.warn("ServiceProvider [{}] not found.", uniqueKey);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("ServiceProvider [{}, {}] is removed.", uniqueKey, provider.getServiceProvider());
            }
        }
        return provider;
    }

    @Override
    public List<ServiceWrapper> getAllServices() {
        return Lists.newArrayList(serviceProviders.values());
    }
}
