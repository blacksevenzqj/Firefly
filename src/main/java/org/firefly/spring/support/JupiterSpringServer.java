package org.firefly.spring.support;

import org.firefly.common.util.ExceptionUtil;
import org.firefly.common.util.Strings;
import org.firefly.common.util.SystemPropertyUtil;
import org.firefly.registry.api.RegistryService;
import org.firefly.rpc.provider.server.DefaultProviderServer;
import org.firefly.rpc.provider.server.JServer;
import org.firefly.transport.api.acceptor.JAcceptor;
import org.springframework.beans.factory.InitializingBean;

import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * 服务端 acceptor wrapper, 负责初始化并启动acceptor.
 */
public class JupiterSpringServer implements InitializingBean {

    private JServer server;
    private RegistryService.RegistryType registryType;
    private JAcceptor acceptor;

    private String registryServerAddresses;             // 注册中心地址 [host1:port1,host2:port2....]
    private boolean hasRegistryServer;                  // true: 需要连接注册中心; false: IP直连方式
//    private ProviderInterceptor[] providerInterceptors; // 全局拦截器
//    private FlowController<JRequest> flowController;    // 全局流量控制

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() {
        server = new DefaultProviderServer(registryType);
        if (acceptor == null) {
            acceptor = createDefaultAcceptor();
        }
        server.withAcceptor(acceptor);

        // 注册中心
        if (Strings.isNotBlank(registryServerAddresses)) {
            server.getRegistryService().connectToRegistryServer(registryServerAddresses);
            hasRegistryServer = true;
        }

        // 全局拦截器
//        if (providerInterceptors != null && providerInterceptors.length > 0) {
//            server.withGlobalInterceptors(providerInterceptors);
//        }

        // 全局限流
//        server.withGlobalFlowController(flowController);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                server.shutdownGracefully();
            }
        });

        try {
            server.start(false);
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }
    }

    public JServer getServer() {
        return server;
    }

    public void setServer(JServer server) {
        this.server = server;
    }

    public RegistryService.RegistryType getRegistryType() {
        return registryType;
    }

    public void setRegistryType(String registryType) {
        this.registryType = RegistryService.RegistryType.parse(registryType);
    }

    public JAcceptor getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(JAcceptor acceptor) {
        this.acceptor = acceptor;
    }

    public String getRegistryServerAddresses() {
        return registryServerAddresses;
    }

    public void setRegistryServerAddresses(String registryServerAddresses) {
        this.registryServerAddresses = registryServerAddresses;
    }

    public boolean isHasRegistryServer() {
        return hasRegistryServer;
    }

    public void setHasRegistryServer(boolean hasRegistryServer) {
        this.hasRegistryServer = hasRegistryServer;
    }

//    public ProviderInterceptor[] getProviderInterceptors() {
//        return providerInterceptors;
//    }
//
//    public void setProviderInterceptors(ProviderInterceptor[] providerInterceptors) {
//        this.providerInterceptors = providerInterceptors;
//    }

//    public FlowController<JRequest> getFlowController() {
//        return flowController;
//    }

//    public void setFlowController(FlowController<JRequest> flowController) {
//        this.flowController = flowController;
//    }

    private JAcceptor createDefaultAcceptor() {
        JAcceptor defaultAcceptor = null;
        try {
            String className = SystemPropertyUtil
                    .get("firefly.io.default.acceptor", "org.firefly.transport.netty.acceptor.ProviderServerNettyTcpAcceptor");
            Class<?> clazz = Class.forName(className);
            defaultAcceptor = (JAcceptor) clazz.newInstance();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }
        return checkNotNull(defaultAcceptor, "default acceptor");
    }

}
