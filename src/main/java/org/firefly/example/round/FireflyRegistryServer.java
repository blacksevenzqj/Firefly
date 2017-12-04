package org.firefly.example.round;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.registry.api.registryserver.RegistryServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FireflyRegistryServer {

//    private static final Logger logger = LoggerFactory.getLogger(FireflyRegistryServer.class);
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(FireflyRegistryServer.class);

    public static void main(String[] args) {
        logger.info("注册服务启动！");
        RegistryServer registryServer = RegistryServer.Default.createRegistryServer(20001, 1);      // 注册中心
        try {
            registryServer.startRegistryServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
