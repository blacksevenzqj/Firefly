package org.firefly.example.round.consumer;

import org.firefly.example.providerservice.ServiceTest;
import org.firefly.example.providerservice.ServiceTest2;
import org.firefly.rpc.consumer.clientserver.DefaultClient;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.ProxyFactory;
import org.firefly.transport.api.connector.JConnector;
import org.firefly.transport.api.exception.ConnectFailedException;
import org.firefly.transport.netty.connector.ConsumerToProviderNettyConnector;

public class SyncFireflyClient {

    public static void main(String[] args) {
        final FClient client = new DefaultClient().withConnector(new ConsumerToProviderNettyConnector());
        // 连接RegistryServer
        client.getRegistryService().connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
//        JConnector.ConnectionWatcher watcher1 = client.watchConnections(ServiceTest.class, "1.0.0.daily");
        JConnector.ConnectionWatcher watcher2 = client.watchConnections(ServiceTest2.class, "1.0.0.daily");
        // 等待 消费者 连接 服务提供者 可用
//        if (!watcher1.waitForAvailable(3000)) {
//            throw new ConnectFailedException();
//        }
        if (!watcher2.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                client.shutdownGracefully();
            }
        });

//        ServiceTest service1 = ProxyFactory.factory(ServiceTest.class)
//                .version("1.0.0.daily")
//                .client(client)
//                .serializerType(SerializerType.JAVA)
//                .clusterStrategy(ClusterInvoker.Strategy.FAIL_OVER)
//                .failoverRetries(5)
//                .newProxyInstance();

//        ServiceTest service1 = ProxyFactory.factory(ServiceTest.class)
//                .version("1.0.0.daily")
//                .client(client)
//                .newProxyInstance();
        System.out.println("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        ServiceTest2 service2 = ProxyFactory.factory(ServiceTest2.class)
                .version("1.0.0.daily")
                .client(client)
                .newProxyInstance();

        try {
//            ServiceTest.ResultClass result1 = service1.sayHello("jupiter", "hello shirt");
//            System.out.println(result1);

            String result2 = service2.sayHelloString();
            System.out.println(result2);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            Thread.currentThread().sleep(10000);
//            JConnector.ConnectionWatcher watcher1 = client.watchConnections(ServiceTest.class, "1.0.0.daily");
//            if (!watcher1.waitForAvailable(3000 * 1200)) {
//                throw new ConnectFailedException();
//            }
//            ServiceTest service1 = ProxyFactory.factory(ServiceTest.class)
//                .version("1.0.0.daily")
//                .client(client)
//                .newProxyInstance();
//
//            ServiceTest.ResultClass result1 = service1.sayHello("jupiter", "hello shirt");
//            System.out.println(result1);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }
}
