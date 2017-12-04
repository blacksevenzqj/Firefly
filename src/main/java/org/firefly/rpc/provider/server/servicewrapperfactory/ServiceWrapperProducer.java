package org.firefly.rpc.provider.server.servicewrapperfactory;

import org.firefly.model.rpc.metadata.ServiceWrapper;
import java.util.concurrent.Executor;

/**
 * 本地服务注册：是个属性接口，相当于一个创建实例的模板接口。
 */
public interface ServiceWrapperProducer {

    /**
     * 设置服务对象
     */
    ServiceWrapperProducer provider(Object serviceProvider);

    /**
     * 设置服务接口类型, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
     */
    ServiceWrapperProducer interfaceClass(Class<?> interfaceClass);

    /**
     * 设置服务组别, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
     */
    ServiceWrapperProducer group(String group);

    /**
     * 设置服务名称, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
     */
    ServiceWrapperProducer providerName(String providerName);

    /**
     * 设置服务版本号, 如果服务接口带 {@link ServiceProvider} 注解, 那么不要再调用此方法, 否则注册会发生异常.
     */
    ServiceWrapperProducer version(String version);

    /**
     * 设置服务权重(0 < weight <= 100).
     */
    ServiceWrapperProducer weight(int weight);

    /**
     * 设置服务提供者私有的线程池, 为了和其他服务提供者资源隔离.
     */
    ServiceWrapperProducer executor(Executor executor);

    /**
     * 注册服务到本地容器.
     */
    ServiceWrapper register();
}
