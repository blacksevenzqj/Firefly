package org.firefly.registry.defaults;

import org.firefly.common.util.Strings;
import org.firefly.common.util.interfice.SpiImpl;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.registry.api.AbstractRegistryService;
import org.firefly.transport.netty.connector.ClientToRegistryNettyConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import static org.firefly.common.util.Preconditions.checkArgument;
import static org.firefly.common.util.Preconditions.checkNotNull;

@SpiImpl(name = "default")
public class DefaultRegistryService extends AbstractRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultRegistryService.class);

    // 服务提供者、消费者 连接 注册服务 各自的 NettyConnector连接保存。
    private final ConcurrentMap<UnresolvedAddress, ClientToRegistryNettyConnector> clients = Maps.newConcurrentMap();

    public DefaultRegistryService(){
        System.out.println("DefaultRegistryService 初始化了");
        initThread();
    }

    @Override
    protected void doSubscribe(RegisterMeta.ServiceMeta serviceMeta) {
        Collection<ClientToRegistryNettyConnector> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Subscribe: {}.", serviceMeta);

        for (ClientToRegistryNettyConnector c : allClients) {
            c.doSubscribe(serviceMeta);
        }
    }

    @Override
    protected void doRegister(RegisterMeta meta) {

        System.out.println("DefaultRegistryService doRegister(RegisterMeta meta) 了1");

        Collection<ClientToRegistryNettyConnector> allClients = clients.values();

        System.out.println("DefaultRegistryService doRegister(RegisterMeta meta) 了2");

        checkArgument(!allClients.isEmpty(), "init needed");

        System.out.println("DefaultRegistryService doRegister(RegisterMeta meta) 了3");

        logger.info("Register: {}.", meta);

        System.out.println("DefaultRegistryService doRegister(RegisterMeta meta) 了4");

        for (ClientToRegistryNettyConnector c : allClients) {
            System.out.println("DefaultRegistryService doRegister(RegisterMeta meta) 了5");
            c.doRegister(meta);
        }
        getRegisterMetaMap().put(meta, RegisterState.DONE);
    }

    @Override
    protected void doUnregister(RegisterMeta meta) {
        Collection<ClientToRegistryNettyConnector> allClients = clients.values();
        checkArgument(!allClients.isEmpty(), "init needed");

        logger.info("Unregister: {}.", meta);

        for (ClientToRegistryNettyConnector c : allClients) {
            c.doUnregister(meta);
        }
    }

    @Override
    protected void doCheckRegisterNodeStatus() {
        // the default registry service does nothing
    }

    @Override
    public void connectToRegistryServer(String connectString) {
        checkNotNull(connectString, "connectString");

        String[] array = Strings.split(connectString, ',');
        for (String s : array) {
            String[] addressStr = Strings.split(s, ':');
            String host = addressStr[0];
            int port = Integer.parseInt(addressStr[1]);
            UnresolvedAddress address = new UnresolvedAddress(host, port);
            ClientToRegistryNettyConnector client = clients.get(address);
            if (client == null) {
                ClientToRegistryNettyConnector newClient = new ClientToRegistryNettyConnector(this);
                client = clients.putIfAbsent(address, newClient);
                if (client == null) {
                    client = newClient;
                    // 服务提供者、消费者 连接 注册服务 后返回的 JConnection实例。
                    // DefaultRegistryService 的 connectToRegistryServer方法 没有对 ClientToRegistryNettyConnector的connect方法返回的JConnection实例做处理。
                    client.connect(address);
                }
            }
        }
    }

    @Override
    public void destroy() {
        for (ClientToRegistryNettyConnector c : clients.values()) {
            c.shutdownGracefully();
        }
    }
}