package org.firefly.rpc.consumer.proxy.dispatch;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.SystemClock;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.rpc.consumer.cluster.MethodSpecialConfig;
import org.firefly.model.rpc.metadata.ServiceMetadata;
import org.firefly.model.rpc.request.JRequest;
import org.firefly.model.rpc.request.JRequestBytes;
import org.firefly.model.rpc.request.MessageWrapper;
import org.firefly.model.rpc.response.JResponse;
import org.firefly.model.rpc.response.ResultWrapper;
import org.firefly.model.rpc.type.DispatchType;
import org.firefly.rpc.tracking.TraceId;
import org.firefly.rpc.tracking.TracingRecorder;
import org.firefly.rpc.tracking.TracingUtil;
import org.firefly.model.transport.channel.CopyOnWriteGroupList;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.model.transport.channel.interfice.JChannelGroup;
import org.firefly.model.transport.configuration.Status;
import org.firefly.rpc.consumer.proxy.balance.interfice.LoadBalancer;
import org.firefly.rpc.consumer.proxy.future.DefaultInvokeFuture;
import org.firefly.rpc.consumer.proxy.future.listener.JFutureListener;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.hook.ConsumerHook;
import org.firefly.rpc.exeption.FireflyRemoteException;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import org.firefly.serialization.SerializerType;

import java.util.List;
import java.util.Map;

import static org.firefly.common.util.exception.StackTraceUtil.stackTrace;

abstract class AbstractDispatcher implements Dispatcher {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractDispatcher.class);

    private final LoadBalancer loadBalancer;                    // 软负载均衡
    private final ServiceMetadata metadata;                     // 目标服务元信息
    private final Serializer serializerImpl;                    // 序列化/反序列化impl
    private ConsumerHook[] hooks = ConsumerHook.EMPTY_HOOKS;    // 消费者端钩子函数
    private long timeoutMillis = JConstants.DEFAULT_TIMEOUT;    // 调用超时时间设置
    // 针对指定方法单独设置的超时时间, 方法名为key, 方法参数类型不做区别对待
    private Map<String, Long> methodSpecialTimeoutMapping = Maps.newHashMap();

    public AbstractDispatcher(ServiceMetadata metadata, SerializerType serializerType) {
        this(null, metadata, serializerType);
    }

    public AbstractDispatcher(LoadBalancer loadBalancer, ServiceMetadata metadata, SerializerType serializerType) {
        this.loadBalancer = loadBalancer;
        this.metadata = metadata;
        this.serializerImpl = SerializerFactory.getSerializer(serializerType.value());
    }

    @Override
    public ServiceMetadata metadata() {
        return metadata;
    }

    public Serializer serializer() {
        return serializerImpl;
    }

    public ConsumerHook[] hooks() {
        return hooks;
    }

    @Override
    public Dispatcher hooks(List<ConsumerHook> hooks) {
        if (hooks != null && !hooks.isEmpty()) {
            this.hooks = hooks.toArray(new ConsumerHook[hooks.size()]);
        }
        return this;
    }

    @Override
    public Dispatcher timeoutMillis(long timeoutMillis) {
        if (timeoutMillis > 0) {
            this.timeoutMillis = timeoutMillis;
        }
        return this;
    }

    @Override
    public Dispatcher methodSpecialConfigs(List<MethodSpecialConfig> methodSpecialConfigs) {
        if (!methodSpecialConfigs.isEmpty()) {
            for (MethodSpecialConfig config : methodSpecialConfigs) {
                long timeoutMillis = config.getTimeoutMillis();
                if (timeoutMillis > 0) {
                    methodSpecialTimeoutMapping.put(config.getMethodName(), timeoutMillis);
                }
            }
        }
        return this;
    }

    public long getMethodSpecialTimeoutMillis(String methodName) {
        Long methodTimeoutMillis = methodSpecialTimeoutMapping.get(methodName);
        if (methodTimeoutMillis != null && methodTimeoutMillis > 0) {
            return methodTimeoutMillis;
        }
        return timeoutMillis;
    }

    protected JChannel select(FClient client) {
        // stack copy
        final ServiceMetadata _metadata = metadata;

        // 根据 ServiceProvider 注解 找到 CopyOnWriteGroupList ---> JChannelGroup[]
        CopyOnWriteGroupList groups = client
                .connector()
                .directory(_metadata);

        // 第一层负载均衡
        JChannelGroup group = loadBalancer.select(groups, _metadata);

        // 第二层负载均衡
        if (group != null) {
            if (group.isAvailable()) { // NettyChannelGroup 中的 CopyOnWriteArrayList<NettyChannel> channels 列表长度 > 0
                return group.next();
            }

            // to the deadline (no available channel), the time exceeded the predetermined limit
            long deadline = group.deadlineMillis();
            if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
                boolean removed = groups.remove(group);
                if (removed) {
                    logger.warn("Removed channel group: {} in directory: {} on [select].", group, _metadata.directory());
                }
            }
        } else {
            // for 3 seconds, expired not wait
            if (!client.awaitConnections(_metadata, 3000)) {
                throw new IllegalStateException("no connections");
            }
        }

        JChannelGroup[] snapshot = groups.snapshot();
        for (JChannelGroup g : snapshot) {
            if (g.isAvailable()) {
                return g.next();
            }
        }

        throw new IllegalStateException("no channel");
    }

    // tracing
    protected MessageWrapper doTracing(MessageWrapper message, JChannel channel) {
        if (TracingUtil.isTracingNeeded()) {
            TraceId traceId = TracingUtil.getCurrent();
            if (traceId == TraceId.NULL_TRACE_ID) {
                traceId = TraceId.newInstance(TracingUtil.generateTraceId());
            }
            message.setTraceId(traceId);

            TracingRecorder recorder = TracingUtil.getRecorder();
            recorder.recording(TracingRecorder.Role.CONSUMER, traceId.asText(), metadata.directory(), message.getMethodName(), channel);
        }
        return message;
    }

    protected <T> DefaultInvokeFuture<T> write(
            JChannel channel, final JRequest request, final DefaultInvokeFuture<T> future, final DispatchType dispatchType) {

        final JRequestBytes requestBytes = request.requestBytes();
        final ConsumerHook[] hooks = future.hooks();

        channel.write(requestBytes, new JFutureListener<JChannel>() {

            @SuppressWarnings("all")
            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                // 标记已发送
                future.markSent();

                if (dispatchType == DispatchType.ROUND) {
                    requestBytes.nullBytes();
                }

                // hook.before()
                for (int i = 0; i < hooks.length; i++) {
                    hooks[i].before(request, channel);
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                if (dispatchType == DispatchType.ROUND) {
                    requestBytes.nullBytes();
                }

                if (logger.isWarnEnabled()) {
                    logger.warn("Writes {} fail on {}, {}.", request, channel, stackTrace(cause));
                }

                ResultWrapper result = new ResultWrapper();
                result.setError(new FireflyRemoteException(cause));

                JResponse response = new JResponse(requestBytes.invokeId());
                response.status(Status.CLIENT_ERROR);
                response.result(result);

                DefaultInvokeFuture.received(channel, response);
            }
        });

        return future;
    }
}
