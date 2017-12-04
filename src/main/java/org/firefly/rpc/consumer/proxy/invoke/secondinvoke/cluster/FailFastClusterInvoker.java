package org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster;

import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.dispatch.Dispatcher;
import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;

/**
 * 快速失败, 只发起一次调用, 失败立即报错(缺省设置)
 * 通常用于非幂等性的写操作.
 */
public class FailFastClusterInvoker extends AbstractClusterInvoker {

    public FailFastClusterInvoker(FClient client, Dispatcher dispatcher) {
        super(client, dispatcher);
    }

    @Override
    public Strategy strategy() {
        return Strategy.FAIL_FAST;
    }

    @Override
    public <T> InvokeFuture<T> invoke(String methodName, Object[] args, Class<T> returnType) throws Exception {
        return dispatcher.dispatch(client, methodName, args, returnType);
    }
}
