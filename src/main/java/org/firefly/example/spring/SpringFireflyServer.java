package org.firefly.example.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 1.启动 SpringRegistryServer
 * 2.再启动 SpringServer
 * 3.最后启动 SpringClient
 */
public class SpringFireflyServer {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:spring-provider.xml");
    }
}
