package org.firefly.common.atomic;

import org.firefly.common.util.internal.JUnsafe;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A tool utility that enables atomic updates to designated {@code volatile} fields of designated classes.
 */
public final class AtomicUpdater {

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param vClass    the class of the field
     * @param fieldName the name of the field to be updated.
     */
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newAtomicReferenceFieldUpdater(
            Class<U> tClass, Class<W> vClass, String fieldName) {
        try {
            return new UnsafeAtomicReferenceFieldUpdater<>(JUnsafe.getUnsafe(), tClass, fieldName);
        } catch (Throwable t) {
            return AtomicReferenceFieldUpdater.newUpdater(tClass, vClass, fieldName);
        }
    }

    private AtomicUpdater() {}
}
