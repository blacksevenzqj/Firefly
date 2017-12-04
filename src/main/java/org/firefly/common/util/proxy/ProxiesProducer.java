package org.firefly.common.util.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.firefly.common.util.Reflects;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import static org.firefly.common.util.Preconditions.checkArgument;

public enum ProxiesProducer {

    JDK_PROXY(new ProxyDelegate() {
        @Override
        public <T> T newProxy(Class<T> interfaceType, Object handler) {
            checkArgument(handler instanceof InvocationHandler, "handler must be a InvocationHandler");
            Object object = Proxy.newProxyInstance(
                    interfaceType.getClassLoader(), new Class<?>[] { interfaceType }, (InvocationHandler) handler);
            return interfaceType.cast(object);
        }
    }),

    BYTE_BUDDY(new ProxyDelegate() {
        @Override
        public <T> T newProxy(Class<T> interfaceType, Object handler) {
            Class<? extends T> cls = new ByteBuddy()
                    .subclass(interfaceType)
                    .method(ElementMatchers.isDeclaredBy(interfaceType))
                    .intercept(MethodDelegation.to(handler, "handler"))
                    .make()
                    .load(interfaceType.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                    .getLoaded();
            return Reflects.newInstance(cls);
        }
    });

    private final ProxyDelegate delegate;

    ProxiesProducer(ProxyDelegate delegate) {
        this.delegate = delegate;
    }

    public static ProxiesProducer parse(String name){
        for (ProxiesProducer s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    public static ProxiesProducer getDefault() {
        return BYTE_BUDDY;
    }

    public <T> T newProxy(Class<T> interfaceType, Object handler) {
        return delegate.newProxy(interfaceType, handler);
    }

    interface ProxyDelegate {
        /**
         * Returns a proxy instance that implements {@code interfaceType} by dispatching
         * method invocations to {@code handler}. The class loader of {@code interfaceType}
         * will be used to define the proxy class.
         */
        <T> T newProxy(Class<T> interfaceType, Object handler);
    }
}
