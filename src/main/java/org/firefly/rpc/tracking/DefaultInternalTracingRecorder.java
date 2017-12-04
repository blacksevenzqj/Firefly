package org.firefly.rpc.tracking;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.StringBuilderHelper;

import java.util.concurrent.TimeUnit;

/**
 * 默认记录tracing信息的方式是打印日志, 可基于SPI扩展.
 */
public class DefaultInternalTracingRecorder extends TracingRecorder {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultInternalTracingRecorder.class);

    @Override
    public void recording(Role role, Object... args) {
        if (logger.isInfoEnabled()) {
            if (role == Role.CONSUMER) {
                String traceInfo = StringBuilderHelper.get()
                        .append("[Consumer] - ")
                        .append(args[0])
                        .append(", callInfo: ")
                        .append(args[1])
                        .append('#')
                        .append(args[2])
                        .append(", on ")
                        .append(args[3]).toString();

                logger.info(traceInfo);
            } else if (role == Role.PROVIDER) {
                String traceInfo = StringBuilderHelper.get()
                        .append("[Provider] - ")
                        .append(args[0])
                        .append(", callInfo: ")
                        .append(args[1])
                        .append(", elapsed: ")
                        .append(TimeUnit.NANOSECONDS.toMillis((long) args[2]))
                        .append(" millis, on ")
                        .append(args[3]).toString();

                logger.info(traceInfo);
            }
        }
    }
}
