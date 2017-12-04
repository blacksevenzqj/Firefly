package org.firefly.example.spring;

import org.firefly.registry.api.registryserver.RegistryServer;

/**
 * 1.启动 SpringRegistryServer
 * 2.再启动 SpringServer
 * 3.最后启动 SpringClient
 */
public class SpringFireflyRegistryServer {

    public static void main(String[] args) {
        RegistryServer registryServer = RegistryServer.Default.createRegistryServer(20001, 1);      // 注册中心
        try {
            registryServer.startRegistryServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
