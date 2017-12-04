package org.firefly.model.rpc.type;

/**
 * 请求派发方式, 支持单播以及广播.
 */
public enum DispatchType {
    ROUND,      // 单播
    BROADCAST;  // 广播

    public static DispatchType parse(String name) {
        for (DispatchType s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    public static DispatchType getDefault() {
        return ROUND;
    }
}
