package org.firefly.rpc.provider.server.servicewrapperfactory;

import org.firefly.common.util.Pair;
import org.firefly.common.util.Strings;
import org.firefly.common.util.internal.Lists;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.rpc.metadata.ServiceWrapper;
import org.firefly.rpc.provider.annotation.ServiceProvider;
import org.firefly.rpc.provider.annotation.ServiceProviderImpl;
import org.firefly.rpc.provider.server.servicecontainer.ServiceProviderContainer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import static org.firefly.common.util.Preconditions.checkArgument;
import static org.firefly.common.util.Preconditions.checkNotNull;

public class DefaultServiceWrapperProducer implements ServiceWrapperProducer {

    // Object serviceProvider：服务提供者实例，如：new ExceptionServiceTestImpl()
    private Object serviceProvider;                     // 服务对象
    private Class<?> interfaceClass;                    // 接口类型
    private String group;                               // 服务组别
    private String providerName;                        // 服务名称
    private String version;                             // 服务版本号, 通常在接口不兼容时版本号才需要升级
    private int weight;                                 // 权重
    private Executor executor;                          // 该服务私有的线程池
    private ServiceProviderContainer providerContainer;

    public DefaultServiceWrapperProducer(ServiceProviderContainer providerContainer) {
        this.providerContainer = providerContainer;
    }

    @Override
    public DefaultServiceWrapperProducer provider(Object serviceProvider) {
        this.serviceProvider = serviceProvider;
        return this;
    }

    @Override
    public DefaultServiceWrapperProducer interfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    @Override
    public DefaultServiceWrapperProducer group(String group) {
        this.group = group;
        return this;
    }

    @Override
    public DefaultServiceWrapperProducer providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    @Override
    public DefaultServiceWrapperProducer version(String version) {
        this.version = version;
        return this;
    }

    @Override
    public DefaultServiceWrapperProducer weight(int weight) {
        this.weight = weight;
        return this;
    }

    @Override
    public DefaultServiceWrapperProducer executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public ServiceWrapper register() {
        // Object serviceProvider：服务提供者实例，如：new ExceptionServiceTestImpl()
        checkNotNull(serviceProvider, "serviceProvider");

        Class<?> providerClass = serviceProvider.getClass(); //得到服务提供者实例的Class。

        ServiceProviderImpl implAnnotation = null;  // 得到ExceptionServiceTestImpl实例上的注解
        ServiceProvider ifAnnotation = null;  // 得到ExceptionServiceTest接口上的注解
        for (Class<?> cls = providerClass; cls != Object.class; cls = cls.getSuperclass()) {
            if (implAnnotation == null) {
                implAnnotation = cls.getAnnotation(ServiceProviderImpl.class);
            }

            Class<?>[] interfaces = cls.getInterfaces();
            if (interfaces != null) {
                for (Class<?> i : interfaces) {
                    ifAnnotation = i.getAnnotation(ServiceProvider.class);
                    if (ifAnnotation == null) {
                        continue;
                    }

                    checkArgument(
                            interfaceClass == null,
                            i.getName() + " has a @ServiceProvider annotation, can't set [interfaceClass] again"
                    );

                    interfaceClass = i;
                    break;
                }
            }

            if (implAnnotation != null && ifAnnotation != null) {
                break;
            }
        }

        // 如果ExceptionServiceTest接口上的注解 ServiceProvider 不为空。
        if (ifAnnotation != null) {
            // 以下检测的目的是：不能手动设置 group 和 providerName 的值，需要从接口的注解上取得，否则抛异常。
            checkArgument(
                    group == null,
                    interfaceClass.getName() + " has a @ServiceProvider annotation, can't set [group] again"
            );
            checkArgument(
                    providerName == null,
                    interfaceClass.getName() + " has a @ServiceProvider annotation, can't set [providerName] again"
            );

            group = ifAnnotation.group();
            String name = ifAnnotation.name();
            providerName = Strings.isNotBlank(name) ? name : interfaceClass.getName();
        }

        // 如果ExceptionServiceTestImpl实例上的注解 ServiceProviderImpl 不会空
        if (implAnnotation != null) {
            // 以下检测的目的是：不能手动设置 version 的值，需要从实现类的注解上取得，否则抛异常。
            checkArgument(
                    version == null,
                    providerClass.getName() + " has a @ServiceProviderImpl annotation, can't set [version] again"
            );

            version = implAnnotation.version();
        }

        checkNotNull(interfaceClass, "interfaceClass");
        checkArgument(Strings.isNotBlank(group), "group");
        checkArgument(Strings.isNotBlank(providerName), "providerName");
        checkArgument(Strings.isNotBlank(version), "version");

        // method's extensions
        //
        // key:     method name
        // value:   pair.first:  方法参数类型(用于根据JLS规则实现方法调用的静态分派)
        //          pair.second: 方法显式声明抛出的异常类型
        Map<String, List<Pair<Class<?>[], Class<?>[]>>> extensions = Maps.newHashMap();
        for (Method method : interfaceClass.getMethods()) {
            String methodName = method.getName();
            List<Pair<Class<?>[], Class<?>[]>> list = extensions.get(methodName);
            if (list == null) {
                list = Lists.newArrayList();
                extensions.put(methodName, list);
            }
            list.add(Pair.of(method.getParameterTypes(), method.getExceptionTypes()));
        }

        return registerService(
                group,
                providerName,
                version,
                serviceProvider,
                extensions,
                weight,
                executor
        );
    }

    private ServiceWrapper registerService(
            String group,
            String providerName,
            String version,
            Object serviceProvider,
            Map<String, List<Pair<Class<?>[], Class<?>[]>>> extensions,
            int weight,
            Executor executor) {

        ServiceWrapper wrapper =
                new ServiceWrapper(group, providerName, version, serviceProvider, extensions);

        wrapper.setWeight(weight);
        wrapper.setExecutor(executor);
        providerContainer.registerService(wrapper.getMetadata().directory(), wrapper);
        return wrapper;
    }

}
