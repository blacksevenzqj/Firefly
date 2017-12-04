package org.firefly.rpc.consumer.proxy;

import org.firefly.common.util.Strings;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.internal.Lists;
import org.firefly.common.util.proxy.ProxiesProducer;
import org.firefly.model.rpc.consumer.cluster.ClusterStrategyConfig;
import org.firefly.model.rpc.consumer.cluster.MethodSpecialConfig;
import org.firefly.model.rpc.metadata.ServiceMetadata;
import org.firefly.model.transport.channel.interfice.JChannelGroup;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.balance.LoadBalancerFactory;
import org.firefly.model.rpc.type.LoadBalancerType;
import org.firefly.rpc.consumer.proxy.dispatch.DefaultRoundDispatcher;
import org.firefly.model.rpc.type.DispatchType;
import org.firefly.rpc.consumer.proxy.dispatch.Dispatcher;
import org.firefly.rpc.consumer.proxy.hook.ConsumerHook;
import org.firefly.model.rpc.type.InvokeType;
import org.firefly.rpc.consumer.proxy.invoke.firestinvoke.bytebuddy.AsyncInvoker;
import org.firefly.rpc.consumer.proxy.invoke.firestinvoke.bytebuddy.SyncInvoker;
import org.firefly.rpc.consumer.proxy.invoke.firestinvoke.generic.AsyncGenericInvoker;
import org.firefly.rpc.consumer.proxy.invoke.firestinvoke.generic.SyncGenericInvoker;
import org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster.ClusterInvoker;
import org.firefly.rpc.provider.annotation.ServiceProvider;
import org.firefly.serialization.SerializerType;
import org.firefly.transport.api.connector.JConnector;
import org.firefly.transport.api.connector.connection.JConnection;
import java.util.Collections;
import java.util.List;

import static org.firefly.common.util.Preconditions.checkArgument;
import static org.firefly.common.util.Preconditions.checkNotNull;



/**
 * Proxy factory
 * Consumer对象代理工厂, [group, providerName, version]
 */
public class ProxyFactory<I> {

    // 接口类型
    private final Class<I> interfaceClass;
    // 服务组别
    private String group;
    // 服务名称
    private String providerName;
    // 服务版本号, 通常在接口不兼容时版本号才需要升级
    private String version;

    // jupiter client
    private FClient client;
    // 序列化/反序列化方式
    private SerializerType serializerType = SerializerType.getDefault();  //default：PROTO_STUFF ((byte) 0x01)
    // 软负载均衡类型
    private LoadBalancerType loadBalancerType = LoadBalancerType.getDefault();  //default：RANDOM 加权随机
    // provider地址
    private List<UnresolvedAddress> addresses;
    // 调用方式 [同步, 异步]
    private InvokeType invokeType = InvokeType.getDefault();  //default：SYNC 同步调用
    // 代理方式[JDK，BYTE_BUDDY]
    private ProxiesProducer proxiesProducer = ProxiesProducer.getDefault(); //default：BYTE_BUDDY

    // 派发方式 [单播, 广播]
    private DispatchType dispatchType = DispatchType.getDefault();  //default：ROUND 单播
    // 调用超时时间设置
    private long timeoutMillis;
    // 指定方法的单独配置, 方法参数类型不做区别对待
    private List<MethodSpecialConfig> methodSpecialConfigs;
    // 消费者端钩子函数
    private List<ConsumerHook> hooks;
    // 集群容错策略
    private ClusterInvoker.Strategy strategy = ClusterInvoker.Strategy.getDefault();  //default：FAIL_FAST 快速失败
    // failover重试次数
    private int retries = 2;

    public static <I> ProxyFactory<I> factory(Class<I> interfaceClass) {
        ProxyFactory<I> factory = new ProxyFactory<>(interfaceClass);
        // 初始化数据
        factory.addresses = Lists.newArrayList();
        factory.hooks = Lists.newArrayList();
        factory.methodSpecialConfigs = Lists.newArrayList();

        return factory;
    }

    private ProxyFactory(Class<I> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Class<I> getInterfaceClass() {
        return interfaceClass;
    }

    public ProxyFactory<I> group(String group) {
        this.group = group;
        return this;
    }

    public ProxyFactory<I> providerName(String providerName) {
        this.providerName = providerName;
        return this;
    }

    public ProxyFactory<I> version(String version) {
        this.version = version;
        return this;
    }

    public ProxyFactory<I> directory(Directory directory) {
        return group(directory.getGroup())
                .providerName(directory.getServiceProviderName())
                .version(directory.getVersion());
    }

    public ProxyFactory<I> client(FClient client) {
        this.client = client;
        return this;
    }

    public ProxyFactory<I> serializerType(SerializerType serializerType) {
        this.serializerType = serializerType;
        return this;
    }

    public ProxyFactory<I> loadBalancerType(LoadBalancerType loadBalancerType) {
        this.loadBalancerType = loadBalancerType;
        return this;
    }

    public ProxyFactory<I> addProviderAddress(UnresolvedAddress... addresses) {
        Collections.addAll(this.addresses, addresses);
        return this;
    }

    public ProxyFactory<I> addProviderAddress(List<UnresolvedAddress> addresses) {
        this.addresses.addAll(addresses);
        return this;
    }

    public ProxyFactory<I> invokeType(InvokeType invokeType) {
        this.invokeType = checkNotNull(invokeType);
        return this;
    }

    public ProxyFactory<I> dispatchType(DispatchType dispatchType) {
        this.dispatchType = checkNotNull(dispatchType);
        return this;
    }

    public ProxyFactory<I> timeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        return this;
    }

    public ProxyFactory<I> addMethodSpecialConfig(MethodSpecialConfig... methodSpecialConfigs) {
        Collections.addAll(this.methodSpecialConfigs, methodSpecialConfigs);
        return this;
    }

    public ProxyFactory<I> addHook(ConsumerHook... hooks) {
        Collections.addAll(this.hooks, hooks);
        return this;
    }

    public ProxyFactory<I> clusterStrategy(ClusterInvoker.Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public ProxyFactory<I> failoverRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public I newProxyInstance() {
        // check arguments
        checkNotNull(interfaceClass, "interfaceClass");

        ServiceProvider annotation = interfaceClass.getAnnotation(ServiceProvider.class);

        if (annotation != null) {
            checkArgument(
                    group == null,
                    interfaceClass.getName() + " has a @ServiceProvider annotation, can't set [group] again"
            );
            checkArgument(
                    providerName == null,
                    interfaceClass.getName() + " has a @ServiceProvider annotation, can't set [providerName] again"
            );

            group = annotation.group();
            String name = annotation.name();
            providerName = Strings.isNotBlank(name) ? name : interfaceClass.getName();
        }

        checkArgument(Strings.isNotBlank(group), "group");
        checkArgument(Strings.isNotBlank(providerName), "providerName");
        checkNotNull(client, "client");
        checkNotNull(serializerType, "serializerType");

        // 如果 分发策略是：广播  且  执行策略是：同步，则：直接异常。
        if (dispatchType == DispatchType.BROADCAST && invokeType == InvokeType.SYNC) {
            throw reject("broadcast & sync unsupported");
        }

        // metadata
        ServiceMetadata metadata = new ServiceMetadata(
                group,
                providerName,
                Strings.isNotBlank(version) ? version : JConstants.DEFAULT_VERSION
        );

        // JNettyTcpConnector -> NettyConnector：DirectoryJChannelGroup
        JConnector<JConnection> connector = client.connector();

        // 手动传进来的控制参数 addresses ：向NettyConnector：DirectoryJChannelGroup中控制的
        // ConcurrentMap<String, CopyOnWriteGroupList> 中的 CopyOnWriteGroupList 添加 JChannelGroup
        for (UnresolvedAddress address : addresses) {
            JChannelGroup jChannelGroup = connector.group(address);
            if(jChannelGroup != null){  // 我改的
                connector.addChannelGroup(metadata, jChannelGroup);
            }else{
                throw reject("ProxyFactory my change address is not in addressGroups");
            }
        }

        // dispatcher
        Dispatcher dispatcher = dispatcher(metadata, serializerType)
                .hooks(hooks)
                .timeoutMillis(timeoutMillis)
                .methodSpecialConfigs(methodSpecialConfigs);

        /**
         * 集群策略配置：
         * 1、集群容错策略ClusterInvoker.Strategy strategy：default：FAIL_FAST 快速失败
         * 2、failover重试次数retries：默认为2次
         */
        ClusterStrategyConfig strategyConfig = ClusterStrategyConfig.of(strategy, retries);

        Object handler;
        switch (invokeType) {  //调度方式：default：SYNC 同步调用
            case SYNC:
                switch(proxiesProducer){
                    case JDK_PROXY:
                        handler = new SyncGenericInvoker(client, dispatcher, strategyConfig, methodSpecialConfigs);
                        break;
                    case BYTE_BUDDY:
                        handler = new SyncInvoker(client, dispatcher, strategyConfig, methodSpecialConfigs);
                        break;
                    default:
                        throw reject("invokeType: " + invokeType + "proxiesProducer：" + proxiesProducer);
                }
                break;
            case ASYNC:
                switch(proxiesProducer){
                    case JDK_PROXY:
                        handler = new AsyncGenericInvoker(client, dispatcher, strategyConfig, methodSpecialConfigs);
                        break;
                    case BYTE_BUDDY:
                        handler = new AsyncInvoker(client, dispatcher, strategyConfig, methodSpecialConfigs);
                        break;
                    default:
                        throw reject("invokeType: " + invokeType + "proxiesProducer：" + proxiesProducer);
                }
                break;
            default:
                throw reject("invokeType: " + invokeType);
        }

        return proxiesProducer.newProxy(interfaceClass, handler);
    }

    protected Dispatcher dispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        switch (dispatchType) {  // 派发方式：default：ROUND 单播
            case ROUND:
                return new DefaultRoundDispatcher(
                        // 软负载均衡类型：default：RANDOM 加权随机
                        LoadBalancerFactory.loadBalancer(loadBalancerType), metadata, serializerType); //default：PROTO_STUFF ((byte) 0x01)
            case BROADCAST:  // 广播
//                return new DefaultBroadcastDispatcher(metadata, serializerType);
            default:
                throw reject("dispatchType: " + dispatchType);
        }
    }

    private static UnsupportedOperationException reject(String message) {
        return new UnsupportedOperationException(message);
    }
}
