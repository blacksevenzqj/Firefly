package org.firefly.transport.api.configuration.template;

import org.firefly.model.transport.configuration.netty.JOption;

import java.util.List;

/**
 * 传输层配置选项, 通常多用于配置网络层参数.
 */
public interface JConfig {

    /**
     * Return all set {@link JOption}'s.
     */
    List<JOption<?>> getOptions();

    /**
     * Return the value of the given {@link JOption}.
     */
    <T> T getOption(JOption<T> option);

    /**
     * Sets a configuration property with the specified name and value.
     *
     * @return {@code true} if and only if the property has been set
     */
    <T> boolean setOption(JOption<T> option, T value);
}
