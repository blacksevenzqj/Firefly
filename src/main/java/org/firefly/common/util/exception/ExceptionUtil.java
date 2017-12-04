package org.firefly.common.util.exception;

import org.firefly.common.util.internal.unsafe.JUnsafe;
import org.firefly.common.util.internal.unsafe.UnsafeReferenceFieldUpdater;
import org.firefly.common.util.internal.unsafe.UnsafeUpdater;
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

        // 找到 cause参数（异常实例）的 异常链中 最顶层异常Throwable。
        Throwable rootCause = cause;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        // 如果 cause参数（异常实例）存在 异常链，并找到了 顶层异常rootCause：
        if (rootCause != cause) {
            // 将 cause参数（异常实例）的栈信息赋值为 顶层异常rootCause的栈信息。
            cause.setStackTrace(rootCause.getStackTrace());  // 1
            /**
             *  使用 Unsafe类：设置 cause参数（异常实例） 中 的异常链 为 cause参数（异常实例）自身 --- 也就是没有了异常链。
             *  解释：Throwable类的异常链为属性：private Throwable cause = this; 那么通过 本类属性 cause_updater --- 使用  Unsafe类 将
             *  cause参数（异常实例）的顶层父类Throwable 中的异常链属性 private Throwable cause 设置为 cause参数（异常实例）本身，清除 异常链。
             */
            assert cause_updater != null;
            cause_updater.set(cause, cause);  // 2、设置 cause参数（异常实例） 中 的异常链 为 cause参数（异常实例）自身
        }
        return cause;
    }

    private ExceptionUtil() {}
}
