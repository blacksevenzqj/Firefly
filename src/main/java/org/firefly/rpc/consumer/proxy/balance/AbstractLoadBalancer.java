package org.firefly.rpc.consumer.proxy.balance;

import org.firefly.common.util.SystemClock;
import org.firefly.model.transport.channel.interfice.JChannelGroup;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.rpc.consumer.proxy.balance.interfice.LoadBalancer;

public abstract class AbstractLoadBalancer implements LoadBalancer {

    private static final ThreadLocal<WeightArray> weightsThreadLocal = new ThreadLocal<WeightArray>() {
        @Override
        protected WeightArray initialValue() {
            return new WeightArray();
        }
    };

    protected WeightArray weightArray(int length) {
        return weightsThreadLocal.get().refresh(length);
    }

    // 计算权重, 包含预热逻辑
    protected int getWeight(JChannelGroup group, Directory directory) {
        int weight = group.getWeight(directory);
        int warmUp = group.getWarmUp();
        int upTime = (int) (SystemClock.millisClock().now() - group.timestamp());

        if (upTime > 0 && upTime < warmUp) {
            // 对端服务预热中, 计算预热权重
            weight = (int) (((float) upTime / warmUp) * weight);
        }

        return weight > 0 ? weight : 0;
    }
}
