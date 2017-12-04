package org.firefly.common.util;

import org.firefly.common.util.internal.InternalThreadLocalMap;

/**
 * 基于 {@link org.firefly.common.util.internal.InternalThreadLocal} 的 {@link StringBuilder} 重复利用.
 *
 * 注意: 不要在相同的线程中嵌套使用, 太大的StringBuilder也请不要使用这个类, 会导致hold超大块内存一直不释放.
 */
public class StringBuilderHelper {

    public static StringBuilder get() {
        return InternalThreadLocalMap.get().stringBuilder();
    }
}
