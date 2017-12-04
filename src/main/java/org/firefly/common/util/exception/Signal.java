package org.firefly.common.util.exception;

import org.firefly.common.util.constant.AbstractConstant;
import org.firefly.common.util.constant.ConstantPool;
import org.firefly.common.util.interfice.Constant;

/**
 * A special {@link Exception} which is used to signal some state or request by throwing it.
 * {@link Signal} has an empty stack trace and has no cause to save the instantiation overhead.
 * 一个特殊的 Exception 抛出 用于：状态信号 或 请求。
 * Signal类：有一个空的堆栈信息，没有必要 保存实例开销。
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>. 返照Netty原生类 Signal。
 */
public final class Signal extends Exception implements Constant<Signal> {

    private static final long serialVersionUID = -221145131122459977L;

    private static final ConstantPool<Signal> pool = new ConstantPool<Signal>() {

        @Override
        protected Signal newConstant(int id, String name) {
            return new Signal(id, name);
        }
    };

    /**
     * Returns the {@link Signal} of the specified name.
     */
    public static Signal valueOf(String name) {
        return pool.valueOf(name);
    }

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */
    public static Signal valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return pool.valueOf(firstNameComponent, secondNameComponent);
    }

    private final SignalConstant constant;

    /**
     * Creates a new {@link Signal} with the specified {@code name}.
     */
    private Signal(int id, String name) {
        constant = new SignalConstant(id, name);
    }

    /**
     * Check if the given {@link Signal} is the same as this instance. If not
     * an {@link IllegalStateException} will be thrown.
     */
    public void expect(Signal signal) {
        if (this != signal) {
            throw new IllegalStateException("unexpected signal: " + signal);
        }
    }

    @Override
    public Throwable initCause(Throwable cause) {
        return this;
    }

    /**
     * 复写 Throwable类的 fillInStackTrace方法，异常堆栈StackTraceElement[] 已实例化，但长度为0。
     * 所以才能 使用 ConstantPool实例中的ConcurrentHashMap来 缓存 Exception的异常子类 Signal。
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public int id() {
        return constant.id();
    }

    @Override
    public String name() {
        return constant.name();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public int compareTo(Signal other) {
        if (this == other) {
            return 0;
        }

        return constant.compareTo(other.constant);
    }

    @Override
    public String toString() {
        return name();
    }

    private static final class SignalConstant extends AbstractConstant<SignalConstant> {
        SignalConstant(int id, String name) {
            super(id, name);
        }
    }
}
