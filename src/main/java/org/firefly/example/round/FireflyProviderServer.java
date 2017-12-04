package org.firefly.example.round;

import org.firefly.example.providerservice.ServiceTest2Impl;
import org.firefly.example.providerservice.ServiceTestImpl;
import org.firefly.model.rpc.metadata.ServiceWrapper;
import org.firefly.rpc.provider.server.DefaultProviderServer;
import org.firefly.rpc.provider.server.JServer;
import org.firefly.transport.netty.acceptor.ProviderServerNettyTcpAcceptor;

public class FireflyProviderServer {

    public static void main(String[] args) {
        final JServer server = new DefaultProviderServer().withAcceptor(new ProviderServerNettyTcpAcceptor(18090));
        try {
            /**
             * 注册到 “服务提供者” 本地容器 DefaultServiceProviderContainer 中的
             *  ConcurrentMap<String, ServiceWrapper> serviceProviders，Key为ServiceMetadata.directory()。
             */
            ServiceWrapper provider1 = server.serviceWrapperProducer()
                    .provider(new ServiceTestImpl())
                    .register();
            // provider2
            ServiceWrapper provider2 = server.serviceWrapperProducer()
                    .provider(new ServiceTest2Impl())
                    .register();

            server.getRegistryService().connectToRegistryServer("127.0.0.1:20001");
            server.publish(provider1);
            server.publish(provider2);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    server.shutdownGracefully();
                }
            });

            server.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
