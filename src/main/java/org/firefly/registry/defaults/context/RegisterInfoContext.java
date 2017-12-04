package org.firefly.registry.defaults.context;

import io.netty.util.internal.ConcurrentSet;
import org.firefly.common.util.internal.Lists;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.registry.ConfigWithVersion;
import org.firefly.model.registry.metadata.RegisterMeta;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * 注册服务的全局信息, 同时也供monitor程序使用.
s */
public class RegisterInfoContext {

    // 指定服务都有哪些节点注册
    private final ConcurrentMap<RegisterMeta.ServiceMeta, ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>>>
            globalRegisterInfoMap = Maps.newConcurrentMap();
    // 指定节点都注册了哪些服务
    private final ConcurrentMap<RegisterMeta.Address, ConcurrentSet<RegisterMeta.ServiceMeta>>
            globalServiceMetaMap = Maps.newConcurrentMap();

    public ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> getRegisterMeta(RegisterMeta.ServiceMeta serviceMeta) {

        ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config = globalRegisterInfoMap.get(serviceMeta);
        if (config == null) {
            ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> newConfig =
                    ConfigWithVersion.newInstance();
            newConfig.setConfig(Maps.<RegisterMeta.Address, RegisterMeta>newConcurrentMap());
            config = globalRegisterInfoMap.putIfAbsent(serviceMeta, newConfig);
            if (config == null) {
                config = newConfig;
            }
        }
        return config;
    }

    public ConcurrentSet<RegisterMeta.ServiceMeta> getServiceMeta(RegisterMeta.Address address) {
        ConcurrentSet<RegisterMeta.ServiceMeta> serviceMetaSet = globalServiceMetaMap.get(address);
        if (serviceMetaSet == null) {
            ConcurrentSet<RegisterMeta.ServiceMeta> newServiceMetaSet = new ConcurrentSet<>();
            serviceMetaSet = globalServiceMetaMap.putIfAbsent(address, newServiceMetaSet);
            if (serviceMetaSet == null) {
                serviceMetaSet = newServiceMetaSet;
            }
        }
        return serviceMetaSet;
    }

    public Object publishLock(ConfigWithVersion<ConcurrentMap<RegisterMeta.Address, RegisterMeta>> config) {
        return checkNotNull(config, "publish lock");
    }

    // - Monitor -------------------------------------------------------------------------------------------------------

    public List<RegisterMeta.Address> listPublisherHosts() {
        return Lists.newArrayList(globalServiceMetaMap.keySet());
    }

    public List<RegisterMeta.Address> listAddressesByService(RegisterMeta.ServiceMeta serviceMeta) {
        return Lists.newArrayList(getRegisterMeta(serviceMeta).getConfig().keySet());
    }

    public List<RegisterMeta.ServiceMeta> listServicesByAddress(RegisterMeta.Address address) {
        return Lists.newArrayList(getServiceMeta(address));
    }
}
