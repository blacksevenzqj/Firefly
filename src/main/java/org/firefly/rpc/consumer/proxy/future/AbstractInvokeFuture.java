package org.firefly.rpc.consumer.proxy.future;

import org.firefly.rpc.consumer.proxy.future.interfice.InvokeFuture;
import org.firefly.rpc.consumer.proxy.future.listener.DefaultListeners;
import org.firefly.rpc.consumer.proxy.future.listener.JListener;
import static org.firefly.common.util.Preconditions.checkNotNull;

@SuppressWarnings("unchecked")
public abstract class AbstractInvokeFuture<V> extends AbstractFuture<V> implements InvokeFuture<V> {

    private Object listeners;

    @Override
    protected void done(int state, Object x) {
        notifyListeners(state, x);
    }

    @Override
    public InvokeFuture<V> addListener(JListener<V> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            addListener0(listener);
        }

        if (isDone()) {
            notifyListeners(state(), outcome());
        }

        return this;
    }

    @Override
    public InvokeFuture<V> addListeners(JListener<V>... listeners) {
        checkNotNull(listeners, "listeners");

        synchronized (this) {
            for (JListener<V> listener : listeners) {
                if (listener == null) {
                    continue;
                }
                addListener0(listener);
            }
        }

        if (isDone()) {
            notifyListeners(state(), outcome());
        }

        return this;
    }

    @Override
    public InvokeFuture<V> removeListener(JListener<V> listener) {
        checkNotNull(listener, "listener");

        synchronized (this) {
            removeListener0(listener);
        }

        return this;
    }

    @Override
    public InvokeFuture<V> removeListeners(JListener<V>... listeners) {
        checkNotNull(listeners, "listeners");

        synchronized (this) {
            for (JListener<V> listener : listeners) {
                if (listener == null) {
                    continue;
                }
                removeListener0(listener);
            }
        }

        return this;
    }

    protected void notifyListeners(int state, Object x) {
        Object listeners;
        synchronized (this) {
            // no competition unless the listener is added too late or the rpc call timeout
            if (this.listeners == null) {
                return;
            }

            listeners = this.listeners;
            this.listeners = null;
        }

        if (listeners instanceof DefaultListeners) {
            JListener<V>[] array = ((DefaultListeners<V>) listeners).listeners();
            int size = ((DefaultListeners<V>) listeners).size();

            for (int i = 0; i < size; i++) {
                notifyListener0(array[i], state, x);
            }
        } else {
            notifyListener0((JListener<V>) listeners, state, x);
        }
    }

    protected abstract void notifyListener0(JListener<V> listener, int state, Object x);

    private void addListener0(JListener<V> listener) {
        if (listeners == null) {
            listeners = listener;
        } else if (listeners instanceof DefaultListeners) {
            ((DefaultListeners<V>) listeners).add(listener);
        } else {
            listeners = DefaultListeners.with((JListener<V>) listeners, listener);
        }
    }

    private void removeListener0(JListener<V> listener) {
        if (listeners instanceof DefaultListeners) {
            ((DefaultListeners<V>) listeners).remove(listener);
        } else if (listeners == listener) {
            listeners = null;
        }
    }
}
