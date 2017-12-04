package org.firefly.rpc.consumer.proxy.invoke.secondinvoke.cluster;

import org.firefly.rpc.consumer.clientserver.FClient;
import org.firefly.rpc.consumer.proxy.dispatch.Dispatcher;

public abstract class AbstractClusterInvoker implements ClusterInvoker {

    protected final FClient client;
    protected final Dispatcher dispatcher;

    public AbstractClusterInvoker(FClient client, Dispatcher dispatcher) {
        this.client = client;
        this.dispatcher = dispatcher;
    }

    @Override
    public String toString() {
        return strategy().name();
    }
}
