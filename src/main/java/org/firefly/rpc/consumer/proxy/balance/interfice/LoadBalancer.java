package org.firefly.rpc.consumer.proxy.balance.interfice;

import org.firefly.model.transport.channel.CopyOnWriteGroupList;
import org.firefly.model.transport.channel.interfice.JChannelGroup;
import org.firefly.model.transport.metadata.Directory;

public interface LoadBalancer {

    /**
     * Select one in elements list.
     *
     * @param groups    elements for select
     * @param directory service directory
     */
    JChannelGroup select(CopyOnWriteGroupList groups, Directory directory);
}
