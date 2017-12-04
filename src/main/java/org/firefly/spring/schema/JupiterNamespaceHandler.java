package org.firefly.spring.schema;

import org.firefly.spring.support.JupiterSpringClient;
import org.firefly.spring.support.JupiterSpringConsumerBean;
import org.firefly.spring.support.JupiterSpringProviderBean;
import org.firefly.spring.support.JupiterSpringServer;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class JupiterNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("server", new JupiterBeanDefinitionParser(JupiterSpringServer.class));
        registerBeanDefinitionParser("client", new JupiterBeanDefinitionParser(JupiterSpringClient.class));
        registerBeanDefinitionParser("provider", new JupiterBeanDefinitionParser(JupiterSpringProviderBean.class));
        registerBeanDefinitionParser("consumer", new JupiterBeanDefinitionParser(JupiterSpringConsumerBean.class));
    }
}
