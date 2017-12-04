package org.firefly.transport.api.configuration.group;

import org.firefly.transport.api.configuration.ChildConfig;
import org.firefly.transport.api.configuration.ParentConfig;
import org.firefly.transport.api.configuration.group.JConfigGroup;

/**
 * TCP netty option
 */
public class NettyTcpConfigGroup implements JConfigGroup {

    private ParentConfig parent = new ParentConfig();
    private ChildConfig child = new ChildConfig();

    @Override
    public ParentConfig parent() {
        return parent;
    }

    @Override
    public ChildConfig child() {
        return child;
    }
}
