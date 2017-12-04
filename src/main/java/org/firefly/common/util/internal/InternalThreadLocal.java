package org.firefly.common.util.internal;

import org.firefly.common.util.exception.ExceptionUtil;

public class InternalThreadLocal<V> {

    /**
     * Returns the number of thread local variables bound to the current thread.
     */
    public static int size() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap == null) {
            return 0;
        } else {
            return threadLocalMap.size();
        }
    }

    public static void destroy() {
        InternalThreadLocalMap.destroy();
    }

    private final int index;

    public InternalThreadLocal() {
        index = InternalThreadLocalMap.nextVariableIndex();
    }

    /**
     * Returns the current value for the current thread
     */
    @SuppressWarnings("unchecked")
    public final V get() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
        Object v = threadLocalMap.indexedVariable(index);
        if (v != InternalThreadLocalMap.UNSET) {
            return (V) v;
        }

        return initialize(threadLocalMap);
    }

    private V initialize(InternalThreadLocalMap threadLocalMap) {
        V v = null;
        try {
            v = initialValue();
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        }

        threadLocalMap.setIndexedVariable(index, v);
        return v;
    }

    /**
     * Sets the value for the current thread.
     */
    public final void set(V value) {
        if (value == null || value == InternalThreadLocalMap.UNSET) {
            remove();
        } else {
            InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.get();
            threadLocalMap.setIndexedVariable(index, value);
        }
    }

    /**
     * Sets the value to uninitialized; a proceeding call to get() will trigger a call to initialValue().
     */
    @SuppressWarnings("unchecked")
    public final void remove() {
        InternalThreadLocalMap threadLocalMap = InternalThreadLocalMap.getIfSet();
        if (threadLocalMap == null) {
            return;
        }

        Object v = threadLocalMap.removeIndexedVariable(index);

        if (v != InternalThreadLocalMap.UNSET) {
            try {
                onRemoval((V) v);
            } catch (Exception e) {
                ExceptionUtil.throwException(e);
            }
        }
    }

    /**
     * Returns the initial value for this thread-local variable.
     */
    protected V initialValue() throws Exception {
        return null;
    }

    /**
     * Invoked when this thread local variable is removed by {@link #remove()}.
     */
    protected void onRemoval(@SuppressWarnings("unused") V value) throws Exception {}
}
