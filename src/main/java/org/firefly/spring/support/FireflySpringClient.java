package org.firefly.spring.support;

import org.firefly.common.util.exception.ExceptionUtil;
import org.firefly.common.util.Strings;
import org.firefly.common.util.SystemPropertyUtil;
import org.firefly.common.util.internal.Lists;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.registry.api.RegistryService;
import org.firefly.rpc.consumer.clientserver.DefaultClient;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.transport.api.connector.JConnector;
import org.firefly.transport.api.connector.connection.JConnection;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.List;

import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * firefly client wrapper, 负责初始化并启动客户端.
 */
public class FireflySpringClient implements InitializingBean {

    private FClient client;
    private String appName;
    private RegistryService.RegistryType registryType;
    private JConnector<JConnection> connector;

    private String registryServerAddresses;                             // 注册中心地址 [host1:port1,host2:port2....]
    private String providerServerAddresses;                             // IP直连到providers [host1:port1,host2:port2....]
    private List<UnresolvedAddress> providerServerUnresolvedAddresses;  // IP直连的地址列表
    private boolean hasRegistryServer;                                  // true: 需要连接注册中心; false: IP直连方式

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        client = new DefaultClient(appName, registryType);
        if (connector == null) {
            connector = createDefaultConnector();
        }
        client.withConnector(connector);

        // 注册中心
        if (Strings.isNotBlank(registryServerAddresses)) {
            client.getRegistryService().connectToRegistryServer(registryServerAddresses);
            hasRegistryServer = true;
        }

        if (!hasRegistryServer) {
            // IP直连方式
            if (Strings.isNotBlank(providerServerAddresses)) {
                String[] array = Strings.split(providerServerAddresses, ',');
                providerServerUnresolvedAddresses = Lists.newArrayList();
                for (String s : array) {
                    String[] addressStr = Strings.split(s, ':');
                    String host = addressStr[0];
                    int port = Integer.parseInt(addressStr[1]);
                    UnresolvedAddress address = new UnresolvedAddress(host, port);
                    providerServerUnresolvedAddresses.add(address);

                    client.connector().connect(address, true); // 异步建立连接
                }
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                client.shutdownGracefully();
            }
        });
    }

    public FClient getClient() {
        return client;
    }

    public void setClient(FClient client) {
        this.client = client;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public RegistryService.RegistryType getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = RegistryService.RegistryType.parse(registryType);
    }

    public JConnector<JConnection> getConnector() {
        return connector;
    }

    public void setConnector(JConnector<JConnection> connector) {
        this.connector = connector;
    }

    public String getRegistryServerAddresses() {
        return registryServerAddresses;
    }

    public void setRegistryServerAddresses(String registryServerAddresses) {
        this.registryServerAddresses = registryServerAddresses;
    }

    public String getProviderServerAddresses() {
        return providerServerAddresses;
    }

    public void setProviderServerAddresses(String providerServerAddresses) {
        this.providerServerAddresses = providerServerAddresses;
    }

    public List<UnresolvedAddress> getProviderServerUnresolvedAddresses() {
        return providerServerUnresolvedAddresses == null
                ?
                Collections.<UnresolvedAddress>emptyList()
                :
                providerServerUnresolvedAddresses;
    }

    public boolean isHasRegistryServer() {
        return hasRegistryServer;
    }

    @SuppressWarnings("unchecked")
    private JConnector<JConnection> createDefaultConnector() {
        JConnector<JConnection> defaultConnector = null;
        try {
            String className = SystemPropertyUtil
                    .get("jupiter.io.default.connector", "org.firefly.transport.netty.connector.ConsumerToProviderNettyConnector");
            Class<?> clazz = Class.forName(className);
            defaultConnector = (JConnector<JConnection>) clazz.newInstance();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }
        return checkNotNull(defaultConnector, "default connector");
    }
}
