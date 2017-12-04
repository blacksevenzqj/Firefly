package org.firefly.rpc.consumer.proxy.future.listener;

import java.util.Arrays;

public class DefaultListeners<V> {

    private JListener<V>[] listeners;
    private int size;

    public static <T> DefaultListeners<T> with(JListener<T> first, JListener<T> second) {
        return new DefaultListeners<>(first, second);
    }

    @SuppressWarnings("unchecked")
    private DefaultListeners(JListener<V> first, JListener<V> second) {
        listeners = new JListener[2];
        listeners[0] = first;
        listeners[1] = second;
        size = 2;
    }

    public void add(JListener<V> l) {
        JListener<V>[] listeners = this.listeners;
        final int size = this.size;
        if (size == listeners.length) {
            this.listeners = listeners = Arrays.copyOf(listeners, size << 1);
        }
        listeners[size] = l;
        this.size = size + 1;
    }

    public void remove(JListener<V> l) {
        final JListener<V>[] listeners = this.listeners;
        int size = this.size;
        for (int i = 0; i < size; i++) {
            if (listeners[i] == l) {
                int length = size - i - 1;
                if (length > 0) {
                    System.arraycopy(listeners, i + 1, listeners, i, length);
                }
                listeners[--size] = null;
                this.size = size;
                return;
            }
        }
    }

    public JListener<V>[] listeners() {
        return listeners;
    }

    public int size() {
        return size;
    }
}
