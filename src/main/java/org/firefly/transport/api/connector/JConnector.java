package org.firefly.transport.api.connector;

import org.firefly.model.transport.channel.CopyOnWriteGroupList;
import org.firefly.model.transport.channel.DirectoryJChannelGroup;
import org.firefly.model.transport.channel.interfice.JChannelGroup;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.model.transport.metadata.UnresolvedAddress;
import org.firefly.transport.api.Transporter;
import org.firefly.transport.api.configuration.template.JConfig;
import org.firefly.transport.api.processor.ConsumerProcessor;
import org.firefly.transport.netty.connector.connection.consumer.JConnectionManager;

import java.util.Collection;

/**
 * 注意 JConnector 单例即可, 不要创建多个实例.
 */
public interface JConnector<C> extends Transporter {

    /**
     * Connector options [parent, child].
     */
    JConfig config();

    /**
     * Binds the rpc processor.
     */
    void withProcessor(ConsumerProcessor processor);

    /**
     * Connects to the remote peer.
     */
    C connect(UnresolvedAddress address);

    /**
     * Connects to the remote peer.
     */
    C connect(UnresolvedAddress address, boolean async);

    /**
     * Returns or new a {@link JChannelGroup}.
     */
    JChannelGroup group(UnresolvedAddress address);

    /**
     * Returns all {@link JChannelGroup}s.
     */
    Collection<JChannelGroup> groups();

    /**
     * Adds a {@link JChannelGroup} by {@link Directory}.
     */
    boolean addChannelGroup(Directory directory, JChannelGroup group);

    /**
     * Removes a {@link JChannelGroup} by {@link Directory}.
     */
    boolean removeChannelGroup(Directory directory, JChannelGroup group);

    /**
     * Returns list of {@link JChannelGroup}s by the same {@link Directory}.
     */
    CopyOnWriteGroupList directory(Directory directory);

    /**
     * Returns {@code true} if has available {@link JChannelGroup}s
     * on this {@link Directory}.
     */
    boolean isDirectoryAvailable(Directory directory);

    /**
     * Returns the {@link DirectoryJChannelGroup}.
     */
    DirectoryJChannelGroup directoryGroup();

    /**
     * Returns the {@link JConnectionManager}.
     */
    JConnectionManager connectionManager();

    /**
     * Shutdown the server.
     */
    void shutdownGracefully();

    interface ConnectionWatcher {

        /**
         * Start to connect to server.
         */
        void start();

        /**
         * Wait until the connections is available or timeout,
         * if available return true, otherwise return false.
         */
        boolean waitForAvailable(long timeoutMillis);
    }
}
