package org.firefly.spring.schema;

import org.firefly.spring.support.FireflySpringClient;
import org.firefly.spring.support.FireflySpringConsumerBean;
import org.firefly.spring.support.FireflySpringProviderBean;
import org.firefly.spring.support.FireflySpringServer;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class FireflyNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("server", new FireflyBeanDefinitionParser(FireflySpringServer.class));
        registerBeanDefinitionParser("client", new FireflyBeanDefinitionParser(FireflySpringClient.class));
        registerBeanDefinitionParser("provider", new FireflyBeanDefinitionParser(FireflySpringProviderBean.class));
        registerBeanDefinitionParser("consumer", new FireflyBeanDefinitionParser(FireflySpringConsumerBean.class));
    }
}
