package org.firefly.transport.api.configuration.group;

import org.firefly.transport.api.configuration.template.JConfig;

/**
 * 对于网络层的服务端,
 * 通常有一个ServerChannel负责监听并接受连接(它的配置选项对应于 {@link #parent()});
 * 还会有N个负责处理read/write等事件的Channel(它的配置选项对应于 {@link #child()});
 */
public interface JConfigGroup {

    /**
     * Config for parent.
     */
    JConfig parent();

    /**
     * Config for child.
     */
    JConfig child();
}
