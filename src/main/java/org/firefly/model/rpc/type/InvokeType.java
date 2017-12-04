package org.firefly.model.rpc.type;

/**
 * 远程调用方式, 支持同步调用和异步调用, 异步方式支持Future以及Listener.
 */
public enum InvokeType {
    SYNC,   // 同步调用
    ASYNC;  // 异步调用

    public static InvokeType parse(String name) {
        for (InvokeType s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    public static InvokeType getDefault() {
        return SYNC;
    }
}
