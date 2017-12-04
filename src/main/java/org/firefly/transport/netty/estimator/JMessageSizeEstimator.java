package org.firefly.transport.netty.estimator;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.FileRegion;
import io.netty.channel.MessageSizeEstimator;
import org.firefly.model.rpc.BytesHolder;

/**
 * 消息size计算, 努力反应真实的IO水位线.
 */
public class JMessageSizeEstimator implements MessageSizeEstimator {

    private static final class HandleImpl implements Handle {
        private final int unknownSize;

        private HandleImpl(int unknownSize) {
            this.unknownSize = unknownSize;
        }

        @Override
        public int size(Object msg) {
            if (msg instanceof ByteBuf) {
                return ((ByteBuf) msg).readableBytes();
            }
            if (msg instanceof ByteBufHolder) {
                return ((ByteBufHolder) msg).content().readableBytes();
            }
            if (msg instanceof FileRegion) {
                return 0;
            }
            // jupiter object
            if (msg instanceof BytesHolder) {
                return ((BytesHolder) msg).size();
            }
            return unknownSize;
        }
    }

    /**
     * Returns the default implementation which returns {@code -1} for unknown messages.
     */
    public static final MessageSizeEstimator DEFAULT = new JMessageSizeEstimator(0);

    private final Handle handle;

    /**
     * Creates a new instance
     *
     * @param unknownSize The size which is returned for unknown messages.
     */
    public JMessageSizeEstimator(int unknownSize) {
        if (unknownSize < 0) {
            throw new IllegalArgumentException("unknownSize: " + unknownSize + " (expected: >= 0)");
        }
        handle = new HandleImpl(unknownSize);
    }

    @Override
    public Handle newHandle() {
        return handle;
    }
}
