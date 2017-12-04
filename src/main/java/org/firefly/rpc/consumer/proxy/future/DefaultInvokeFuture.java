package org.firefly.rpc.consumer.proxy.future;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.Signal;
import org.firefly.common.util.constant.JConstants;
import org.firefly.common.util.internal.Maps;
import org.firefly.model.rpc.response.JResponse;
import org.firefly.model.rpc.response.ResultWrapper;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.model.transport.configuration.Status;
import org.firefly.model.rpc.type.DispatchType;
import org.firefly.rpc.consumer.proxy.future.listener.JListener;
import org.firefly.rpc.consumer.proxy.hook.ConsumerHook;
import org.firefly.rpc.exeption.FireflyBizException;
import org.firefly.rpc.exeption.FireflyRemoteException;
import org.firefly.rpc.exeption.FireflySerializationException;
import org.firefly.rpc.exeption.FireflyTimeoutException;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.firefly.common.util.StackTraceUtil.stackTrace;
import static org.firefly.common.util.Preconditions.checkNotNull;


@SuppressWarnings("all")
public class DefaultInvokeFuture<V> extends AbstractInvokeFuture<V> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultInvokeFuture.class);

    private static final long DEFAULT_TIMEOUT_NANOSECONDS = TimeUnit.MILLISECONDS.toNanos(JConstants.DEFAULT_TIMEOUT);

    private static final ConcurrentMap<Long, DefaultInvokeFuture<?>> roundFutures = Maps.newConcurrentMap();
    private static final ConcurrentMap<String, DefaultInvokeFuture<?>> broadcastFutures = Maps.newConcurrentMap();

    private final long invokeId; // request.invokeId, 广播的场景可以重复
    private final JChannel channel;
    private final Class<V> returnType;
    private final long timeout;
    private final long startTime = System.nanoTime();

    private volatile boolean sent = false;

    private ConsumerHook[] hooks = ConsumerHook.EMPTY_HOOKS;

    public static <T> DefaultInvokeFuture<T> with(
            long invokeId, JChannel channel, Class<T> returnType, long timeoutMillis, DispatchType dispatchType) {

        return new DefaultInvokeFuture<T>(invokeId, channel, returnType, timeoutMillis, dispatchType);
    }

    private DefaultInvokeFuture(
            long invokeId, JChannel channel, Class<V> returnType, long timeoutMillis, DispatchType dispatchType) {

        this.invokeId = invokeId;
        this.channel = channel;
        this.returnType = returnType;
        this.timeout = timeoutMillis > 0 ? TimeUnit.MILLISECONDS.toNanos(timeoutMillis) : DEFAULT_TIMEOUT_NANOSECONDS;

        switch (dispatchType) {
            case ROUND:
                roundFutures.put(invokeId, this);
                break;
            case BROADCAST:
                broadcastFutures.put(subInvokeId(channel, invokeId), this);
                break;
            default:
                throw new IllegalArgumentException("unsupported " + dispatchType);
        }
    }

    @Override
    public Class<V> returnType() {
        return returnType;
    }

    @Override
    public V getResult() throws Throwable {
        try {
            return get(timeout, TimeUnit.NANOSECONDS);
        } catch (Signal s) {
            SocketAddress address = channel.remoteAddress();
            if (s == TIMEOUT) {
                throw new FireflyTimeoutException(address, sent ? Status.SERVER_TIMEOUT : Status.CLIENT_TIMEOUT);
            } else {
                throw new FireflyRemoteException(s.name(), address);
            }
        }
    }

    @Override
    protected void notifyListener0(JListener<V> listener, int state, Object x) {
        try {
            if (state == NORMAL) {
                listener.complete((V) x);
            } else {
                listener.failure((Throwable) x);
            }
        } catch (Throwable t) {
            logger.error("An exception was thrown by {}.{}, {}.",
                    listener.getClass().getName(), state == NORMAL ? "complete()" : "failure()", stackTrace(t));
        }
    }

    public void markSent() {
        sent = true;
    }

    public ConsumerHook[] hooks() {
        return hooks;
    }

    public DefaultInvokeFuture<V> hooks(ConsumerHook[] hooks) {
        checkNotNull(hooks, "hooks");

        this.hooks = hooks;
        return this;
    }

    private void doReceived(JResponse response) {
        byte status = response.status();

        if (status == Status.OK.value()) {
            ResultWrapper wrapper = response.result();
            set((V) wrapper.getResult());
        } else {
            setException(status, response);
        }

        // call hook's after method
        for (int i = 0; i < hooks.length; i++) {
            hooks[i].after(response, channel);
        }
    }

    private void setException(byte status, JResponse response) {
        Throwable cause;
        if (status == Status.SERVER_TIMEOUT.value()) {
            cause = new FireflyTimeoutException(channel.remoteAddress(), Status.SERVER_TIMEOUT);
        } else if (status == Status.CLIENT_TIMEOUT.value()) {
            cause = new FireflyTimeoutException(channel.remoteAddress(), Status.CLIENT_TIMEOUT);
        } else if (status == Status.DESERIALIZATION_FAIL.value()) {
            ResultWrapper wrapper = response.result();
            cause = (FireflySerializationException) wrapper.getResult();
        } else if (status == Status.SERVICE_EXPECTED_ERROR.value()) {
            ResultWrapper wrapper = response.result();
            cause = (Throwable) wrapper.getResult();
        } else if (status == Status.SERVICE_UNEXPECTED_ERROR.value()) {
            ResultWrapper wrapper = response.result();
            String message = String.valueOf(wrapper.getResult());
            cause = new FireflyBizException(message, channel.remoteAddress());
        } else {
            ResultWrapper wrapper = response.result();
            Object result = wrapper.getResult();
            if (result != null && result instanceof FireflyRemoteException) {
                cause = (FireflyRemoteException) result;
            } else {
                cause = new FireflyRemoteException(response.toString(), channel.remoteAddress());
            }
        }
        setException(cause);
    }

    // 服务提供者 返回信息时 调用
    public static void received(JChannel channel, JResponse response) {
        long invokeId = response.id();
        DefaultInvokeFuture<?> future = roundFutures.remove(invokeId);
        if (future == null) {
            // 广播场景下做出了一点让步, 多查询了一次roundFutures
            future = broadcastFutures.remove(subInvokeId(channel, invokeId));
        }
        if (future == null) {
            logger.warn("A timeout response [{}] finally returned on {}.", response, channel);
            return;
        }

        future.doReceived(response);
    }

    private static String subInvokeId(JChannel channel, long invokeId) {
        return channel.id() + invokeId;
    }

    // timeout scanner
    @SuppressWarnings("all")
    private static class TimeoutScanner implements Runnable {

        public void run() {
            for (;;) {
                try {
                    // round
                    for (DefaultInvokeFuture<?> future : roundFutures.values()) {
                        process(future);
                    }

                    // broadcast
                    for (DefaultInvokeFuture<?> future : broadcastFutures.values()) {
                        process(future);
                    }
                } catch (Throwable t) {
                    logger.error("An exception was caught while scanning the timeout futures {}.", stackTrace(t));
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {}
            }
        }

        private void process(DefaultInvokeFuture<?> future) {
            if (future == null || future.isDone()) {
                return;
            }

            if (System.nanoTime() - future.startTime > future.timeout) {
                JResponse response = new JResponse(future.invokeId);
                response.status(future.sent ? Status.SERVER_TIMEOUT : Status.CLIENT_TIMEOUT);

                DefaultInvokeFuture.received(future.channel, response);
            }
        }
    }

    static {
        Thread t = new Thread(new TimeoutScanner(), "timeout.scanner");
        t.setDaemon(true);
        t.start();
    }
}
