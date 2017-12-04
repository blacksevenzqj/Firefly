package org.firefly.model.rpc.metadata;

import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.Pair;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * Wrapper provider object and service metadata.
 * 服务元数据 & 服务对象
 */
public class ServiceWrapper implements Serializable {

    private static final long serialVersionUID = 6690575889849847348L;

    // 服务元数据
    private final ServiceMetadata metadata;
    // 服务对象
    private final Object serviceProvider;
    // key:     method name
    // value:   pair.first:  方法参数类型(用于根据JLS规则实现方法调用的静态分派)
    //          pair.second: 方法显式声明抛出的异常类型
    private final Map<String, List<Pair<Class<?>[], Class<?>[]>>> extensions;

    // 权重 hashCode() 与 equals() 不把weight计算在内
    private int weight = JConstants.DEFAULT_WEIGHT;
    // provider私有线程池
    private Executor executor;

    public ServiceWrapper(String group,
                          String providerName,
                          String version,
                          Object serviceProvider,
                          Map<String, List<Pair<Class<?>[], Class<?>[]>>> extensions) {

        metadata = new ServiceMetadata(group, providerName, version);

        this.extensions = checkNotNull(extensions, "extensions");
        this.serviceProvider = checkNotNull(serviceProvider, "serviceProvider");
    }

    public ServiceMetadata getMetadata() {
        return metadata;
    }

    public Object getServiceProvider() {
        return serviceProvider;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public List<Pair<Class<?>[], Class<?>[]>> getMethodExtension(String methodName) {
        return extensions.get(methodName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceWrapper wrapper = (ServiceWrapper) o;

        return metadata.equals(wrapper.metadata);
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    @Override
    public String toString() {
        return "ServiceWrapper{" +
                "metadata=" + metadata +
                ", serviceProvider=" + serviceProvider +
                ", extensions=" + extensions +
                ", weight=" + weight +
                ", executor=" + executor +
                '}';
    }
}
