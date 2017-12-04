package org.firefly.spring.schema;

import org.firefly.common.util.Strings;
import org.firefly.common.util.internal.Lists;
import org.firefly.model.rpc.consumer.cluster.ClusterStrategyConfig;
import org.firefly.model.rpc.consumer.cluster.MethodSpecialConfig;
import org.firefly.spring.support.JupiterSpringClient;
import org.firefly.spring.support.JupiterSpringConsumerBean;
import org.firefly.spring.support.JupiterSpringProviderBean;
import org.firefly.spring.support.JupiterSpringServer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;

public class JupiterBeanDefinitionParser implements BeanDefinitionParser {

    private final Class<?> beanClass;

    public JupiterBeanDefinitionParser(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        if (beanClass == JupiterSpringServer.class) {
            return parseJupiterServer(element, parserContext);
        } else if (beanClass == JupiterSpringClient.class) {
            return parseJupiterClient(element, parserContext);
        } else if (beanClass == JupiterSpringProviderBean.class) {
            return parseJupiterProvider(element, parserContext);
        } else if (beanClass == JupiterSpringConsumerBean.class) {
            return parseJupiterConsumer(element, parserContext);
        } else {
            throw new BeanDefinitionValidationException("Unknown class to definition: " + beanClass.getName());
        }
    }

    private BeanDefinition parseJupiterServer(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addProperty(def, element, "registryType", false);
        addPropertyReference(def, element, "acceptor", false);

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "registryServerAddresses", false);
                    addPropertyReference(def, (Element) item, "providerInterceptors", false);
                    addPropertyReference(def, (Element) item, "flowController", false);
                }
            }
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseJupiterClient(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addProperty(def, element, "appName", false);
        addProperty(def, element, "registryType", false);
        addPropertyReference(def, element, "connector", false);

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "registryServerAddresses", false);
                    addProperty(def, (Element) item, "providerServerAddresses", false);
                }
            }
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseJupiterProvider(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addPropertyReference(def, element, "server", true);
        addPropertyReference(def, element, "providerImpl", true);

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "weight", false);
                    addPropertyReference(def, (Element) item, "providerInterceptors", false);
                    addPropertyReference(def, (Element) item, "executor", false);
                    addPropertyReference(def, (Element) item, "flowController", false);
                    addPropertyReference(def, (Element) item, "providerInitializer", false);
                    addPropertyReference(def, (Element) item, "providerInitializerExecutor", false);
                }
            }
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition parseJupiterConsumer(Element element, ParserContext parserContext) {
        RootBeanDefinition def = new RootBeanDefinition();
        def.setBeanClass(beanClass);

        addPropertyReference(def, element, "client", true);
        addProperty(def, element, "interfaceClass", true);

        List<MethodSpecialConfig> methodSpecialConfigs = Lists.newArrayList();

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item instanceof Element) {
                String localName = item.getLocalName();
                if ("property".equals(localName)) {
                    addProperty(def, (Element) item, "version", false);
                    addProperty(def, (Element) item, "serializerType", false);
                    addProperty(def, (Element) item, "loadBalancerType", false);
                    addProperty(def, (Element) item, "waitForAvailableTimeoutMillis", false);
                    addProperty(def, (Element) item, "invokeType", false);
                    addProperty(def, (Element) item, "dispatchType", false);
                    addProperty(def, (Element) item, "timeoutMillis", false);
                    addProperty(def, (Element) item, "providerAddresses", false);
                    addProperty(def, (Element) item, "clusterStrategy", false);
                    addProperty(def, (Element) item, "failoverRetries", false);
                    addPropertyReference(def, (Element) item, "hooks", false);
                } else if ("methodSpecials".equals(localName)) {
                    NodeList configList = item.getChildNodes();
                    for (int j = 0; j < configList.getLength(); j++) {
                        Node configItem = configList.item(j);
                        if (configItem instanceof Element) {
                            if ("methodSpecial".equals(configItem.getLocalName())) {
                                String methodName = ((Element) configItem).getAttribute("methodName");
                                String timeoutMillis = ((Element) configItem).getAttribute("timeoutMillis");
                                String clusterStrategy = ((Element) configItem).getAttribute("clusterStrategy");
                                String failoverRetries = ((Element) configItem).getAttribute("failoverRetries");

                                MethodSpecialConfig config = MethodSpecialConfig.of(methodName)
                                        .timeoutMillis(Long.parseLong(timeoutMillis))
                                        .strategy(ClusterStrategyConfig.of(clusterStrategy, failoverRetries));
                                methodSpecialConfigs.add(config);
                            }
                        }
                    }
                }
            }
        }

        if (!methodSpecialConfigs.isEmpty()) {
            def.getPropertyValues().addPropertyValue("methodSpecialConfigs", methodSpecialConfigs);
        }

        return registerBean(def, element, parserContext);
    }

    private BeanDefinition registerBean(RootBeanDefinition definition, Element element, ParserContext parserContext) {
        String id = element.getAttribute("id");
        if (Strings.isNullOrEmpty(id)) {
            id = beanClass.getSimpleName();
        }
        if (parserContext.getRegistry().containsBeanDefinition(id)) {
            throw new IllegalStateException("Duplicate jupiter bean id: " + id);
        }

        BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id);
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());

        return definition;
    }

    private static void addProperty(RootBeanDefinition definition, Element element, String propertyName, boolean required) {
        String ref = element.getAttribute(propertyName);
        if (required) {
            checkAttribute(propertyName, ref);
        }
        if (!Strings.isNullOrEmpty(ref)) {
            definition.getPropertyValues().addPropertyValue(propertyName, ref);
        }
    }

    private static void addPropertyReference(RootBeanDefinition definition, Element element, String propertyName, boolean required) {
        String ref = element.getAttribute(propertyName);
        if (required) {
            checkAttribute(propertyName, ref);
        }
        if (!Strings.isNullOrEmpty(ref)) {
            definition.getPropertyValues().addPropertyValue(propertyName, new RuntimeBeanReference(ref));
        }
    }

    private static String checkAttribute(String attributeName, String attribute) {
        if (Strings.isNullOrEmpty(attribute)) {
            throw new BeanDefinitionValidationException("attribute [" + attributeName + "] is required.");
        }
        return attribute;
    }
}
