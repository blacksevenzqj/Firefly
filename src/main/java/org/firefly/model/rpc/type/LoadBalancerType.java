package org.firefly.model.rpc.type;

public enum LoadBalancerType {
    ROUND_ROBIN,    // 加权轮询
    RANDOM;         // 加权随机

    public static LoadBalancerType parse(String name) {
        for (LoadBalancerType s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    public static LoadBalancerType getDefault() {
        return RANDOM;
    }
}
