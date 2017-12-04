package org.firefly.rpc.provider.processor;

import org.firefly.common.util.exception.ExceptionUtil;
import org.firefly.model.rpc.request.JRequest;
import org.firefly.model.rpc.request.JRequestBytes;
import org.firefly.model.rpc.response.JResponseBytes;
import org.firefly.model.rpc.response.ResultWrapper;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.rpc.consumer.proxy.future.listener.JFutureListener;
import org.firefly.model.transport.configuration.Status;
import org.firefly.rpc.provider.LookupService;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import org.firefly.transport.api.processor.ProviderProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.firefly.common.util.exception.StackTraceUtil.stackTrace;

public abstract class AbstractProviderProcessor implements ProviderProcessor, LookupService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProviderProcessor.class);

    @Override
    public void handleException(JChannel channel, JRequestBytes request, Status status, Throwable cause) {
        logger.error("An exception was caught while processing request: {}, {}.",
                channel.remoteAddress(), stackTrace(cause));

        doHandleException(
                channel, request.invokeId(), request.serializerCode(), status.value(), cause, false);
    }

    public void handleException(JChannel channel, JRequest request, Status status, Throwable cause) {
        logger.error("An exception was caught while processing request: {}, {}.",
                channel.remoteAddress(), stackTrace(cause));

        doHandleException(
                channel, request.invokeId(), request.serializerCode(), status.value(), cause, false);
    }

    public void handleRejected(JChannel channel, JRequest request, Status status, Throwable cause) {
        if (logger.isWarnEnabled()) {
            logger.warn("Service rejected: {}, {}.", channel.remoteAddress(), stackTrace(cause));
        }

        doHandleException(
                channel, request.invokeId(), request.serializerCode(), status.value(), cause, true);
    }

    private void doHandleException(
            JChannel channel, long invokeId, byte s_code, byte status, Throwable cause, boolean closeChannel) {

        ResultWrapper result = new ResultWrapper();
        // 截断cause, 避免客户端无法找到cause类型而无法序列化（清除 cause异常实例 的 异常链）
        cause = ExceptionUtil.cutCause(cause);
        result.setError(cause);

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        byte[] bytes = serializer.writeObject(result);

        JResponseBytes response = new JResponseBytes(invokeId);
        response.status(status);
        response.bytes(s_code, bytes);

        if (closeChannel) {
            channel.write(response, JChannel.CLOSE);
        } else {
            channel.write(response, new JFutureListener<JChannel>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    logger.debug("Service error message sent out: {}.", channel);
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Service error message sent failed: {}, {}.", channel, stackTrace(cause));
                    }
                }
            });
        }
    }
}
