package org.firefly.rpc.provider.processor.task;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.concurrent.thread.executorfactory.rejected.RejectedRunnable;
import org.firefly.common.util.*;
import org.firefly.common.util.internal.UnsafeIntegerFieldUpdater;
import org.firefly.common.util.internal.UnsafeUpdater;
import org.firefly.model.rpc.metadata.ServiceMetadata;
import org.firefly.model.rpc.metadata.ServiceWrapper;
import org.firefly.model.rpc.request.JRequest;
import org.firefly.model.rpc.request.JRequestBytes;
import org.firefly.model.rpc.request.MessageWrapper;
import org.firefly.model.rpc.response.JResponseBytes;
import org.firefly.model.rpc.response.ResultWrapper;
import org.firefly.rpc.tracking.TraceId;
import org.firefly.rpc.tracking.TracingRecorder;
import org.firefly.rpc.tracking.TracingUtil;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.rpc.consumer.proxy.future.listener.JFutureListener;
import org.firefly.model.transport.configuration.Status;
import org.firefly.rpc.exeption.FireflyBadRequestException;
import org.firefly.rpc.exeption.FireflyRemoteException;
import org.firefly.rpc.exeption.FireflyServerBusyException;
import org.firefly.rpc.exeption.FireflyServiceNotFoundException;
import org.firefly.rpc.metric.Metrics;
import org.firefly.rpc.provider.processor.AbstractProviderProcessor;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class ProviderMessageTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProviderMessageTask.class);

    private static final boolean METRIC_NEEDED = SystemPropertyUtil.getBoolean("firefly.metric.needed", true);

    private static final UnsafeIntegerFieldUpdater<TraceId> traceNodeUpdater =
            UnsafeUpdater.newIntegerFieldUpdater(TraceId.class, "node");

    private final AbstractProviderProcessor processor;
    private final JChannel channel;
    private final JRequest request;

    public ProviderMessageTask(AbstractProviderProcessor processor, JChannel channel, JRequest request) {
        this.processor = processor;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        // stack copy
        final AbstractProviderProcessor _processor = processor;
        final JRequest _request = request;

        MessageWrapper msg;
        try {
            JRequestBytes _requestBytes = _request.requestBytes();

            byte s_code = _requestBytes.serializerCode();
            byte[] bytes = _requestBytes.bytes();
            _requestBytes.nullBytes();

            if (METRIC_NEEDED) {
                MetricsHolder.requestSizeHistogram.update(bytes.length);
            }

            Serializer serializer = SerializerFactory.getSerializer(s_code);
            // 在业务线程中反序列化, 减轻IO线程负担
            msg = serializer.readObject(bytes, MessageWrapper.class);
            _request.message(msg);
        } catch (Throwable t) {
            rejected(Status.BAD_REQUEST, new FireflyBadRequestException(t.getMessage()));
            return;
        }

        // 查找服务
        final ServiceWrapper service = _processor.lookupService(msg.getMetadata());
        if (service == null) {
            rejected(Status.SERVICE_NOT_FOUND, new FireflyServiceNotFoundException(String.valueOf(msg)));
            return;
        }

        // processing
        Executor childExecutor = service.getExecutor();
        if (childExecutor == null) {
            process(service);
        } else {
            // provider私有线程池执行
            childExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    process(service);
                }
            });
        }
    }

    @Override
    public void rejected() {
        rejected(Status.SERVER_BUSY, new FireflyServerBusyException(String.valueOf(request)));
    }

    private void rejected(Status status, FireflyRemoteException cause) {
        if (METRIC_NEEDED) {
            MetricsHolder.rejectionMeter.mark();
        }

        // 当服务拒绝方法被调用时一般分以下几种情况:
        //  1. 非法请求, close当前连接;
        //  2. 服务端处理能力出现瓶颈, close当前连接, jupiter客户端会自动重连, 在加权负载均衡的情况下权重是一点一点升上来的.
        processor.handleRejected(channel, request, status, cause);
    }

    private void process(ServiceWrapper service) {
        // stack copy
        final JRequest _request = request;

        try {
            MessageWrapper msg = _request.message();
            String methodName = msg.getMethodName();
            TraceId traceId = msg.getTraceId();
            System.out.println("!!! ProviderMessageTask process traceId !!! " +  traceId);

            // bind current traceId
            if (TracingUtil.isTracingNeeded()) {
                bindCurrentTraceId(traceId);
            }

            Object provider = service.getServiceProvider();
            Object[] args = msg.getArgs();

            String callInfo = null;
            Timer.Context timerCtx = null;
            if (METRIC_NEEDED) {
                callInfo = getCallInfo(msg.getMetadata(), methodName);
                timerCtx = Metrics.timer(callInfo).time();
            }

            Object invokeResult = null;
            Throwable failCause = null;
            Class<?>[] exceptionTypes = null;
            try {
                // key:     method name
                // value:   pair.first:  方法参数类型(用于根据JLS规则实现方法调用的静态分派)
                //          pair.second: 方法显式声明抛出的异常类型
                List<Pair<Class<?>[], Class<?>[]>> methodExtension = service.getMethodExtension(methodName);
                if (methodExtension == null) {
                    throw new NoSuchMethodException(methodName);
                }

                // 根据JLS方法调用的静态分派规则查找最匹配的方法parameterTypes
                Pair<Class<?>[], Class<?>[]> bestMatch = Reflects.findMatchingParameterTypesExt(methodExtension, args);
                Class<?>[] parameterTypes = bestMatch.getFirst();
                exceptionTypes = bestMatch.getSecond();
                invokeResult = Reflects.fastInvoke(provider, methodName, parameterTypes, args);
            } catch (Throwable t) {
                // handle biz exception
                handleException(exceptionTypes, failCause = t);
                return;
            } finally {
                long elapsed = -1;
                if (METRIC_NEEDED) {
                    elapsed = timerCtx.stop();
                }

                // tracing recoding
                if (traceId != null && TracingUtil.isTracingNeeded()) {
                    if (callInfo == null) {
                        callInfo = getCallInfo(msg.getMetadata(), methodName);
                    }
                    TracingRecorder recorder = TracingUtil.getRecorder();
                    recorder.recording(TracingRecorder.Role.PROVIDER, traceId.asText(), callInfo, elapsed, channel);
                }
            }

            ResultWrapper result = new ResultWrapper();
            result.setResult(invokeResult);
            byte s_code = _request.serializerCode();
            Serializer serializer = SerializerFactory.getSerializer(s_code);
            byte[] bytes = serializer.writeObject(result);

            if (METRIC_NEEDED) {
                MetricsHolder.responseSizeHistogram.update(bytes.length);
            }

            JResponseBytes response = new JResponseBytes(_request.invokeId());
            response.status(Status.OK.value());
            response.bytes(s_code, bytes);

            handleWriteResponse(response);
        } catch (Throwable t) {
            processor.handleException(channel, _request, Status.SERVER_ERROR, t);
        }
    }

    private void handleWriteResponse(JResponseBytes response) {
        channel.write(response, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                if (METRIC_NEEDED) {
                    MetricsHolder.processingTimer.update(
                            SystemClock.millisClock().now() - request.timestamp(), TimeUnit.MILLISECONDS);
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                logger.error(
                        "Service response[traceId: {}] sent failed, elapsed: {} millis, channel: {}, cause: {}.",
                        request.message().getTraceId(), SystemClock.millisClock().now() - request.timestamp(), channel, cause
                );
            }
        });
    }

    private void handleException(Class<?>[] exceptionTypes, Throwable failCause) {
        if (exceptionTypes != null && exceptionTypes.length > 0) {
            Class<?> failType = failCause.getClass();
            for (Class<?> eType : exceptionTypes) {
                // 如果抛出声明异常的子类, 客户端可能会因为不存在子类类型而无法序列化, 会在客户端抛出无法反序列化异常
                if (eType.isAssignableFrom(failType)) {
                    // 预期内的异常
                    processor.handleException(channel, request, Status.SERVICE_EXPECTED_ERROR, failCause);
                    return;
                }
            }
        }

        // 预期外的异常
        processor.handleException(channel, request, Status.SERVICE_UNEXPECTED_ERROR, failCause);
    }

    private static void bindCurrentTraceId(TraceId traceId) {
        if (traceId != null) {
            assert traceNodeUpdater != null;
            traceNodeUpdater.set(traceId, traceId.getNode() + 1);
        }
        TracingUtil.setCurrent(traceId);
    }

    private static String getCallInfo(ServiceMetadata metadata, String methodName) {
        String directory = metadata.directory();
        return StringBuilderHelper.get()
                .append(directory)
                .append('#')
                .append(methodName).toString();
    }

    // - Metrics -------------------------------------------------------------------------------------------------------
    static class MetricsHolder {
        // 请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)
        static final Timer processingTimer              = Metrics.timer("processing");
        // 请求被拒绝次数统计
        static final Meter rejectionMeter               = Metrics.meter("rejection");
        // 请求数据大小统计(不包括Jupiter协议头的16个字节)
        static final Histogram requestSizeHistogram     = Metrics.histogram("request.size");
        // 响应数据大小统计(不包括Jupiter协议头的16个字节)
        static final Histogram responseSizeHistogram    = Metrics.histogram("response.size");
    }
}
