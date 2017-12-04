package org.firefly.common.util;

import org.firefly.common.util.internal.JUnsafe;
import org.firefly.common.util.internal.UnsafeReferenceFieldUpdater;
import org.firefly.common.util.internal.UnsafeUpdater;
import sun.misc.Unsafe;

public class ExceptionUtil {

    private static final UnsafeReferenceFieldUpdater<Throwable, Throwable> cause_updater =
            UnsafeUpdater.newReferenceFieldUpdater(Throwable.class, "cause");

    /**
     * Raises an exception bypassing compiler checks for checked exceptions.
     */
    public static void throwException(Throwable t) {
        Unsafe unsafe = JUnsafe.getUnsafe();
        if (unsafe != null) {
            unsafe.throwException(t);
        } else {
            ExceptionUtil.<RuntimeException>throwException0(t);
        }
    }

    /**
     * 类型转换只是骗过前端javac编译器, 泛型只是个语法糖, 在javac编译后会解除语法糖将类型擦除,
     * 也就是说并不会生成checkcast指令, 所以在运行期不会抛出ClassCastException异常
     *
     * private static <E extends java/lang/Throwable> void throwException0(java.lang.Throwable) throws E;
     *      flags: ACC_PRIVATE, ACC_STATIC
     *      Code:
     *      stack=1, locals=1, args_size=1
     *          0: aload_0
     *          1: athrow // 注意在athrow之前并没有checkcast指令
     *      ...
     *  Exceptions:
     *      throws java.lang.Throwable
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwException0(Throwable t) throws E {
        throw (E) t;
    }

    public static <T extends Throwable> T cutCause(T cause) {
        Throwable rootCause = cause;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        if (rootCause != cause) {
            cause.setStackTrace(rootCause.getStackTrace());
            assert cause_updater != null;
            cause_updater.set(cause, cause);
        }
        return cause;
    }

    private ExceptionUtil() {}
}
