package org.firefly.rpc.consumer.proxy.future;

import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;

import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * 异步调用上下文, 用于获取当前上下文中的 {@link InvokeFuture}, 基于 {@link ThreadLocal}.
 */
@SuppressWarnings("unchecked")
public class InvokeFutureContext {

    private static final ThreadLocal<InvokeFuture<?>> futureThreadLocal = new ThreadLocal<>();

    /**
     * 获取单播/广播调用的 {@link InvokeFuture}, 不协助类型转换.
     */
    public static InvokeFuture<?> future() {
        InvokeFuture<?> future = checkNotNull(futureThreadLocal.get(), "future");
        futureThreadLocal.remove();
        return future;
    }

    /**
     * 获取单播调用的 {@link InvokeFuture} 并协助类型转换, {@code expectReturnType} 为期望定的返回值类型.
     */
    public static <V> InvokeFuture<V> future(Class<V> expectReturnType) {
        InvokeFuture<?> f = future();
        checkReturnType(f.returnType(), expectReturnType);

        return (InvokeFuture<V>) f;
    }

    public static void set(InvokeFuture<?> future) {
        futureThreadLocal.set(future);
    }

    private static void checkReturnType(Class<?> realType, Class<?> expectType) {
        if (!expectType.isAssignableFrom(realType)) {
            throw new IllegalArgumentException(
                    "illegal returnType, expect type is ["
                            + expectType.getName()
                            + "], but real type is ["
                            + realType.getName() + "]");
        }
    }
}
