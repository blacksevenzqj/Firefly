package org.firefly.transport.netty.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.concurrent.thread.NamedThreadFactory;
import org.firefly.common.util.ClassUtil;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.transport.channel.CopyOnWriteGroupList;
import org.firefly.model.transport.channel.DirectoryJChannelGroup;
import org.firefly.model.transport.channel.NettyChannelGroup;
import org.firefly.model.transport.channel.interfice.JChannelGroup;
import org.firefly.model.transport.configuration.netty.JOption;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.transport.api.Transporter;
import org.firefly.transport.api.configuration.template.JConfig;
import org.firefly.transport.api.connector.JConnector;
import org.firefly.transport.api.connector.connection.JConnection;
import org.firefly.transport.api.processor.ConsumerProcessor;
import org.firefly.transport.netty.connector.connection.consumer.JConnectionManager;
import org.firefly.transport.netty.estimator.JMessageSizeEstimator;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import static org.firefly.common.util.Preconditions.checkNotNull;

public abstract class NettyConnector implements JConnector<JConnection> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NettyConnector.class);

    static {
        // touch off DefaultChannelId.<clinit>
        // because getProcessId() sometimes too slow
        ClassUtil.classInitialize("io.netty.channel.DefaultChannelId", 500);
    }

    protected final Transporter.Protocol protocol;
    protected final HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("connector.timer"));

    // addressGroups 第一次添加 应该在 ConsumerToProviderConnectionWatcher类的 notify方法中
    private final ConcurrentMap<UnresolvedAddress, JChannelGroup> addressGroups = Maps.newConcurrentMap();

    private final DirectoryJChannelGroup directoryGroup = new DirectoryJChannelGroup();
    private final JConnectionManager connectionManager = new JConnectionManager();

    private Bootstrap bootstrap;
    private EventLoopGroup worker;
    private int nWorkers;

    protected volatile ByteBufAllocator allocator;

    public NettyConnector(Protocol protocol) {
        this(protocol, JConstants.AVAILABLE_PROCESSORS + 1);
    }

    public NettyConnector(Protocol protocol, int nWorkers) {
        this.protocol = protocol;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory workerFactory = workerThreadFactory("firefly.connector");
        worker = initEventLoopGroup(nWorkers, workerFactory);

        bootstrap = new Bootstrap().group(worker);

        JConfig child = config();
        child.setOption(JOption.IO_RATIO, 100);
        child.setOption(JOption.PREFER_DIRECT, true);
        child.setOption(JOption.USE_POOLED_ALLOCATOR, true);

        doInit();
    }

    protected abstract void doInit();

    protected ThreadFactory workerThreadFactory(String name) {
        return new DefaultThreadFactory(name, Thread.MAX_PRIORITY);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public void withProcessor(ConsumerProcessor processor) {
        // the default implementation does nothing
    }

    @Override
    public JChannelGroup group(UnresolvedAddress address) {
        checkNotNull(address, "address");

        JChannelGroup group = addressGroups.get(address);
        if (group == null) {
            JChannelGroup newGroup = channelGroup(address);
            group = addressGroups.putIfAbsent(address, newGroup);
            if (group == null) {
                group = newGroup;
            }
        }
        return group;
    }

    @Override
    public Collection<JChannelGroup> groups() {
        return addressGroups.values();
    }

    @Override
    public boolean addChannelGroup(Directory directory, JChannelGroup group) {
        CopyOnWriteGroupList groups = directory(directory);
        boolean added = groups.addIfAbsent(group);
        if (added) {
            logger.info("Added channel group: {} to {}.", group, directory.directory());
        }
        return added;
    }

    @Override
    public boolean removeChannelGroup(Directory directory, JChannelGroup group) {
        CopyOnWriteGroupList groups = directory(directory);
        boolean removed = groups.remove(group);
        if (removed) {
            logger.warn("Removed channel group: {} in directory: {}.", group, directory.directory());
        }
        return removed;
    }

    @Override
    public CopyOnWriteGroupList directory(Directory directory) {
        return directoryGroup.find(directory);
    }

    @Override
    public boolean isDirectoryAvailable(Directory directory) {
        CopyOnWriteGroupList groups = directory(directory);
        JChannelGroup[] snapshot = groups.snapshot();
        for (JChannelGroup g : snapshot) {
            if (g.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public DirectoryJChannelGroup directoryGroup() {
        return directoryGroup;
    }

    @Override
    public JConnectionManager connectionManager() {
        return connectionManager;
    }

    @Override
    public void shutdownGracefully() {
        connectionManager.cancelAllReconnect();
        worker.shutdownGracefully();
    }

    protected void setOptions() {
        JConfig child = config();

        setIoRatio(child.getOption(JOption.IO_RATIO));

        boolean direct = child.getOption(JOption.PREFER_DIRECT);
        if (child.getOption(JOption.USE_POOLED_ALLOCATOR)) {
            if (direct) {
                allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new PooledByteBufAllocator(false);
            }
        } else {
            if (direct) {
                allocator = new UnpooledByteBufAllocator(PlatformDependent.directBufferPreferred());
            } else {
                allocator = new UnpooledByteBufAllocator(false);
            }
        }
        bootstrap.option(ChannelOption.ALLOCATOR, allocator)
                .option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, JMessageSizeEstimator.DEFAULT);
    }

    /**
     * A {@link Bootstrap} that makes it easy to bootstrap a {@link io.netty.channel.Channel} to use
     * for clients.
     */
    protected Bootstrap bootstrap() {
        return bootstrap;
    }

    /**
     * Lock object with bootstrap.
     */
    protected Object bootstrapLock() {
        return bootstrap;
    }

    /**
     * The {@link EventLoopGroup} for the child. These {@link EventLoopGroup}'s are used to handle
     * all the events and IO for {@link io.netty.channel.Channel}'s.
     */
    protected EventLoopGroup worker() {
        return worker;
    }

    /**
     * Creates the same address of the channel group.
     */
    protected JChannelGroup channelGroup(UnresolvedAddress address) {
        System.out.println("NettyConnector are channelGroup：又新建了一个JChannelGroup");
        return new NettyChannelGroup(address);
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.
     * The default value is {@code 50}, which means the event loop will try to spend the same
     * amount of time for I/O as for non-I/O tasks.
     */
    public abstract void setIoRatio(int workerIoRatio);

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory}.
     */
    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);
}
