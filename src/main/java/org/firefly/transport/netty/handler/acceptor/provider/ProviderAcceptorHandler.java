package org.firefly.transport.netty.handler.acceptor.provider;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.firefly.common.util.exception.Signal;
import org.firefly.model.rpc.request.JRequestBytes;
import org.firefly.model.transport.channel.NettyChannel;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.model.transport.configuration.Status;
import org.firefly.transport.api.processor.ProviderProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


@ChannelHandler.Sharable
public class ProviderAcceptorHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ProviderAcceptorHandler.class);

    private static final AtomicInteger channelCounter = new AtomicInteger(0);

    private ProviderProcessor processor;

    public ProviderProcessor processor() {
        return processor;
    }
    public void processor(ProviderProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (msg instanceof JRequestBytes) {
            JChannel jChannel = NettyChannel.attachChannel(ch);
            try {
                processor.handleRequest(jChannel, (JRequestBytes) msg);
            } catch (Throwable t) {
                processor.handleException(jChannel, (JRequestBytes) msg, Status.SERVER_ERROR, t);
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);
            }

            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.incrementAndGet();

        logger.info("Connects with {} as the {}th channel.", ctx.channel(), count);

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int count = channelCounter.getAndDecrement();

        logger.warn("Disconnects with {} as the {}th channel.", ctx.channel(), count);

        super.channelInactive(ctx);
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
            logger.error("An I/O exception was caught: {}, force to close channel: {}.", cause, ch);

            ch.close();
        } else {
            logger.error("An unexpected exception was caught: {}, channel: {}.", cause, ch);
        }
    }

}
