package org.firefly.rpc.consumer.proxy.balance;

import org.firefly.model.rpc.type.LoadBalancerType;
import org.firefly.rpc.consumer.proxy.balance.interfice.LoadBalancer;

public final class LoadBalancerFactory {

    public static LoadBalancer loadBalancer(LoadBalancerType type) {
        if (type == LoadBalancerType.RANDOM) {
            return RandomLoadBalancer.instance();
        }

        if (type == LoadBalancerType.ROUND_ROBIN) {
            return RoundRobinLoadBalancer.instance();
        }

        // 如果不指定, 默认的负载均衡算法是加权随机
        return RandomLoadBalancer.instance();
    }

    private LoadBalancerFactory() {}
}
