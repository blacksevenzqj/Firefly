package org.firefly.rpc.consumer.proxy.invoke.firestinvoke.generic;

/**
 * 简单的理解, 泛化调用就是不依赖二方包, 通过传入方法名, 方法参数值, 就可以调用服务.
 */
public interface GenericInvoker {

    Object invoke(String methodName, Object... args) throws Throwable;
}
