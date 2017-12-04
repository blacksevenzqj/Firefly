package org.firefly.rpc.tracking;

public abstract class TracingRecorder {

    public abstract void recording(Role role, Object... args);

    public enum Role {
        CONSUMER,
        PROVIDER
    }
}
