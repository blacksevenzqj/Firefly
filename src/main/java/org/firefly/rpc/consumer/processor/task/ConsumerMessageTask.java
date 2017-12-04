package org.firefly.rpc.consumer.processor.task;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.model.rpc.response.JResponse;
import org.firefly.model.rpc.response.JResponseBytes;
import org.firefly.model.rpc.response.ResultWrapper;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.model.transport.configuration.Status;
import org.firefly.rpc.consumer.proxy.future.DefaultInvokeFuture;
import org.firefly.rpc.exeption.FireflySerializationException;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import static org.firefly.common.util.exception.StackTraceUtil.stackTrace;

public class ConsumerMessageTask implements Runnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConsumerMessageTask.class);

    private final JChannel channel;
    private final JResponse response;

    public ConsumerMessageTask(JChannel channel, JResponse response) {
        this.channel = channel;
        this.response = response;
    }

    @Override
    public void run() {
        // stack copy
        final JResponse _response = response;
        final JResponseBytes _responseBytes = _response.responseBytes();

        byte s_code = _response.serializerCode();
        byte[] bytes = _responseBytes.bytes();
        _responseBytes.nullBytes();

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        ResultWrapper wrapper;
        try {
            wrapper = serializer.readObject(bytes, ResultWrapper.class);
        } catch (Throwable t) {
            logger.error("Deserialize object failed: {}, {}.", channel.remoteAddress(), stackTrace(t));

            _response.status(Status.DESERIALIZATION_FAIL);
            wrapper = new ResultWrapper();
            wrapper.setError(new FireflySerializationException(t));
        }
        _response.result(wrapper);

        DefaultInvokeFuture.received(channel, _response);
    }
}
