package org.firefly.registry.api.consumer;

import org.firefly.model.registry.metadata.RegisterMeta;

/**
 * Service subscribers listener.
 */
public interface NotifyListener {

    void notify(RegisterMeta registerMeta, NotifyEvent event);

    enum NotifyEvent {
        CHILD_ADDED,
        CHILD_REMOVED
    }
}
