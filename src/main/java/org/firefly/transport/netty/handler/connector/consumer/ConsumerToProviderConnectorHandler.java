package org.firefly.transport.netty.handler.connector.consumer;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.Signal;
import org.firefly.model.rpc.response.JResponseBytes;
import org.firefly.model.transport.channel.NettyChannel;
import org.firefly.transport.api.processor.ConsumerProcessor;
import java.io.IOException;
import static org.firefly.common.util.StackTraceUtil.stackTrace;

@ChannelHandler.Sharable
public class ConsumerToProviderConnectorHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConsumerToProviderConnectorHandler.class);

    private ConsumerProcessor processor;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (msg instanceof JResponseBytes) {
            try {
                processor.handleResponse(NettyChannel.attachChannel(ch), (JResponseBytes) msg);
            } catch (Throwable t) {
                logger.error("An exception was caught: {}, on {} #channelRead().", stackTrace(t), ch);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);
            }

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();
        ChannelConfig config = ch.config();

        // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
        // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
        if (!ch.isWritable()) {
            // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
            if (logger.isWarnEnabled()) {
                logger.warn("{} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(false);
        } else {
            // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
            if (logger.isWarnEnabled()) {
                logger.warn("{} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                        ch, config.getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel ch = ctx.channel();

        if (cause instanceof Signal) {
            logger.error("An I/O signal was caught: {}, force to close channel: {}.", ((Signal) cause).name(), ch);

            ch.close();
        } else if (cause instanceof IOException) {
            logger.error("An I/O exception was caught: {}, force to close channel: {}.", stackTrace(cause), ch);

            ch.close();
        } else {
            logger.error("An unexpected exception was caught: {}, channel: {}.", stackTrace(cause), ch);
        }
    }

    public ConsumerProcessor processor() {
        return processor;
    }

    public void processor(ConsumerProcessor processor) {
        this.processor = processor;
    }
}
