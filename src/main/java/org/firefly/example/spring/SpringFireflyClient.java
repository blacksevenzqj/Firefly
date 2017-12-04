package org.firefly.example.spring;

import org.firefly.example.providerservice.ServiceTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 1.启动 SpringRegistryServer
 * 2.再启动 SpringServer
 * 3.最后启动 SpringClient
 */
public class SpringFireflyClient {

    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:spring-consumer.xml");
        ServiceTest service = ctx.getBean(ServiceTest.class);
        try {
            ServiceTest.ResultClass result1 = service.sayHello("jupiter");
            System.out.println(result1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
