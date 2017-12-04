package org.firefly.transport.netty.connector.connection.consumer;

import org.firefly.common.util.ExceptionUtil;
import org.firefly.model.registry.metadata.RegisterMeta;
import org.firefly.model.transport.channel.interfice.JChannelGroup;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.registry.api.consumer.NotifyListener;
import org.firefly.registry.api.consumer.OfflineListener;
import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.transport.api.connector.JConnector;
import org.firefly.transport.api.connector.connection.JConnection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConsumerToProviderConnectionWatcher implements JConnector.ConnectionWatcher{

    private final FClient fClient;
    private final Directory directory;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notifyCondition = lock.newCondition();
    // attempts to elide conditional wake-ups when the lock is uncontended.
    private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

    public ConsumerToProviderConnectionWatcher(FClient fClient, Directory directory) {
        this.fClient = fClient;
        this.directory = directory;
    }

    @Override
    public void start() {
        fClient.subscribe(directory, new NotifyListener() {
            @Override
            public void notify(RegisterMeta registerMeta, NotifyEvent event) {
                UnresolvedAddress address = new UnresolvedAddress(registerMeta.getHost(), registerMeta.getPort());
                final JChannelGroup group = fClient.getConnector().group(address);
                if (event == NotifyEvent.CHILD_ADDED) {
                    if (!group.isAvailable()) {
                        // 当“消费者”请求订阅“注册服务”返回服务信息后，“消费者”依次连接“服务提供者”。连接信息封装为JConnection数组。
                        JConnection[] connections = connectTo(address, group, registerMeta, true);
                        for (JConnection c : connections) {
                            /**
                             * 1、设置JNettyConnection中operationComplete方法参数：Runnable线程实例（ChannelFuture属性异步监听的 执行线程Runnable实现）
                             * 2、执行线程Runnable实现 中的onSucceed方法 逻辑为：向NettyConnector：DirectoryJChannelGroup中控制的
                             * ConcurrentMap<String, CopyOnWriteGroupList> 中的 CopyOnWriteGroupList 添加 JChannelGroup
                             * 3、调用 JNettyConnection 中 operationComplete方法：触发 ChannelFuture属性 异步监听 ---> 执行线程Runnable实现 ---> 调用onSucceed方法
                             */
                            c.operationComplete(new Runnable() {
                                @Override
                                public void run() {
                                    onSucceed(group, signalNeeded.getAndSet(false));
                                }
                            });
                        }
                    } else {
                        onSucceed(group, signalNeeded.getAndSet(false));
                    }
                    // 这里设置的权重是指：一个JChannelGroup对应一个服务提供者，当有“服务提供者”集群时，就有多个JChannelGroup，这时就需要设置JChannelGroup的权重。
                    group.setWeight(directory, registerMeta.getWeight()); // 设置权重
                } else if (event == NotifyEvent.CHILD_REMOVED) {
                    fClient.getConnector().removeChannelGroup(directory, group);
                    group.removeWeight(directory);
                    if (fClient.getConnector().directoryGroup().getRefCount(group) <= 0) {
                        fClient.getConnector().connectionManager().cancelReconnect(address); // 取消自动重连
                    }
                }
            }
        });
    }

    private JConnection[] connectTo(final UnresolvedAddress address, final JChannelGroup group, RegisterMeta registerMeta, boolean async) {
        int connCount = registerMeta.getConnCount();
        connCount = connCount < 1 ? 1 : connCount;
        // 抽象类数据，不是实例化抽象类。
        JConnection[] connections = new JConnection[connCount];
        group.setCapacity(connCount);
        for (int i = 0; i < connCount; i++) {
            // 根据 服务地址 连接“服务提供者”
            JConnection connection =  fClient.getConnector().connect(address, async);
            connections[i] = connection;
            fClient.getConnector().connectionManager().manage(connection);

            fClient.offlineListening(address, new OfflineListener() {
                @Override
                public void offline() {
                    fClient.getConnector().connectionManager().cancelReconnect(address); // 取消自动重连
                    if (!group.isAvailable()) {
                        fClient.getConnector().removeChannelGroup(directory, group);
                    }
                }
            });
        }

        return connections;
    }

    private void onSucceed(JChannelGroup group, boolean doSignal) {  // doSignal 为 false
        // 2、执行 ChannelFuture 监听中的 Runnable实现 中的onSucceed方法 逻辑为：向NettyConnector：DirectoryJChannelGroup中控制的
        // ConcurrentMap<String, CopyOnWriteGroupList> 中的 CopyOnWriteGroupList 添加 JChannelGroup
        fClient.getConnector().addChannelGroup(directory, group);
        if (doSignal) {
            final ReentrantLock _look = lock;
            _look.lock();
            try {
                notifyCondition.signalAll();
            } finally {
                _look.unlock();
            }
        }
    }


    @Override
    public boolean waitForAvailable(long timeoutMillis) {
        if (fClient.getConnector().isDirectoryAvailable(directory)) {
            return true;
        }
        long remains = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        boolean available = false;
        final ReentrantLock _look = lock;
        _look.lock();
        try {
            signalNeeded.set(true);
            // avoid "spurious wakeup" occurs
            while (!(available = fClient.getConnector().isDirectoryAvailable(directory))) {
                if ((remains = notifyCondition.awaitNanos(remains)) <= 0) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            ExceptionUtil.throwException(e);
        } finally {
            _look.unlock();
        }

        return available || fClient.getConnector().isDirectoryAvailable(directory);
    }
}
