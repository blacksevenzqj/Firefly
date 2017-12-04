package org.firefly.rpc.consumer.proxy.future.interfice;

import org.firefly.rpc.consumer.proxy.future.listener.JListener;

/**
 * A future that accepts completion listeners.
 */
@SuppressWarnings("unchecked")
public interface ListenableFuture<V> {

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * done.  If this future is already completed, the
     * specified listener is notified immediately.
     */
    ListenableFuture<V> addListener(JListener<V> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * done.  If this future is already completed, the
     * specified listeners are notified immediately.
     */
    ListenableFuture<V> addListeners(JListener<V>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is done.  If the specified listener is not associated
     * with this future, this method does nothing and returns silently.
     */
    ListenableFuture<V> removeListener(JListener<V> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is done.  If the specified listeners are not associated
     * with this future, this method does nothing and returns silently.
     */
    ListenableFuture<V> removeListeners(JListener<V>... listeners);
}
