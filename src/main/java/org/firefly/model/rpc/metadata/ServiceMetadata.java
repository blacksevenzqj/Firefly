package org.firefly.model.rpc.metadata;

import org.firefly.model.transport.metadata.Directory;

import java.io.Serializable;
import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * Service provider's metadata.
 *
 * 服务的元数据.
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
public class ServiceMetadata extends Directory implements Serializable {

    private static final long serialVersionUID = -8908295634641380163L;

    private String group;               // 服务组别
    private String serviceProviderName; // 服务名称
    private String version;             // 服务版本号

    public ServiceMetadata() {}

    public ServiceMetadata(String group, String serviceProviderName, String version) {
        this.group = checkNotNull(group, "group");
        this.serviceProviderName = checkNotNull(serviceProviderName, "serviceProviderName");
        this.version = checkNotNull(version, "version");
    }

    @Override
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String getServiceProviderName() {
        return serviceProviderName;
    }

    public void setServiceProviderName(String serviceProviderName) {
        this.serviceProviderName = serviceProviderName;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceMetadata metadata = (ServiceMetadata) o;

        return group.equals(metadata.group)
                && serviceProviderName.equals(metadata.serviceProviderName)
                && version.equals(metadata.version);
    }

    @Override
    public int hashCode() {
        int result = group.hashCode();
        result = 31 * result + serviceProviderName.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ServiceMetadata{" +
                "group='" + group + '\'' +
                ", serviceProviderName='" + serviceProviderName + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
