package org.firefly.common.util;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class ClassUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClassUtil.class);

    /**
     * 提前加载并初始化指定的类, 某些平台下某些类的静态块里面的代码执行实在是太慢了:(
     *
     * @param className         类的全限定名称
     * @param tolerableMillis   超过这个时间打印警告日志
     */
    public static void classInitialize(String className, long tolerableMillis) {
        long start = System.currentTimeMillis();
        try {
            Class.forName(className);
        } catch (Throwable t) {
            logger.warn("Failed to load class [{}] {}.", className, t);
        }

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > tolerableMillis) {
            logger.warn("{}.<clinit> elapsed: {} millis.", className, elapsed);
        }
    }

    public static void classCheck(String className) {
        try {
            Class.forName(className);
        } catch (Throwable t) {
            logger.error("Failed to load class [{}] {}.", className, t);
            ExceptionUtil.throwException(t);
        }
    }
}
