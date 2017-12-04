package org.firefly.transport.api.configuration;

import org.firefly.common.util.internal.Lists;
import org.firefly.model.transport.configuration.netty.JOption;
import org.firefly.transport.api.configuration.template.NettyConfig;

import java.util.Collections;
import java.util.List;
import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * TCP netty parent option
 */
public class ParentConfig extends NettyConfig {

    private volatile int backlog = 1024;
    private volatile int rcvBuf = -1;
    private volatile boolean reuseAddress = true;

    @Override
    public List<JOption<?>> getOptions() {
        return getOptions(super.getOptions(),
                JOption.SO_BACKLOG,
                JOption.SO_RCVBUF,
                JOption.SO_REUSEADDR);
    }

    protected List<JOption<?>> getOptions(List<JOption<?>> result, JOption<?>... options) {
        if (result == null) {
            result = Lists.newArrayList();
        }
        Collections.addAll(result, options);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getOption(JOption<T> option) {
        checkNotNull(option);

        if (option == JOption.SO_BACKLOG) {
            return (T) Integer.valueOf(getBacklog());
        }
        if (option == JOption.SO_RCVBUF) {
            return (T) Integer.valueOf(getRcvBuf());
        }
        if (option == JOption.SO_REUSEADDR) {
            return (T) Boolean.valueOf(isReuseAddress());
        }

        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(JOption<T> option, T value) {
        validate(option, value);

        if (option == JOption.SO_BACKLOG) {
            setIoRatio((Integer) value);
        } else if (option == JOption.SO_RCVBUF) {
            setRcvBuf((Integer) value);
        } else if (option == JOption.SO_REUSEADDR) {
            setReuseAddress((Boolean) value);
        } else {
            return super.setOption(option, value);
        }

        return true;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getRcvBuf() {
        return rcvBuf;
    }

    public void setRcvBuf(int rcvBuf) {
        this.rcvBuf = rcvBuf;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }
}
