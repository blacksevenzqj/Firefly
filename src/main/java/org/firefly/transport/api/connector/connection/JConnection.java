package org.firefly.transport.api.connector.connection;

import org.firefly.model.transport.metadata.UnresolvedAddress;

public abstract class JConnection {

    private final UnresolvedAddress address;

    public JConnection(UnresolvedAddress address) {
        this.address = address;
    }

    public UnresolvedAddress getAddress() {
        return address;
    }

    public void operationComplete(@SuppressWarnings("unused") Runnable callback) {
        // the default implementation does nothing
    }

    public abstract void setReconnect(boolean reconnect);
}
