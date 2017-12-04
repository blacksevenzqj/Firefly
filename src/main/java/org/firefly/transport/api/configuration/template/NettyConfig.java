package org.firefly.transport.api.configuration.template;

import org.firefly.common.util.internal.Lists;
import org.firefly.model.transport.configuration.netty.JOption;

import java.util.Collections;
import java.util.List;
import static org.firefly.common.util.Preconditions.checkNotNull;

public class NettyConfig implements JConfig {

    private volatile int ioRatio = 100;
    private volatile boolean preferDirect = true;
    private volatile boolean usePooledAllocator = true;

    @Override
    public List<JOption<?>> getOptions() {
        return getOptions(null,
                JOption.IO_RATIO,
                JOption.PREFER_DIRECT,
                JOption.USE_POOLED_ALLOCATOR);
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

        if (option == JOption.IO_RATIO) {
            return (T) Integer.valueOf(getIoRatio());
        }
        if (option == JOption.PREFER_DIRECT) {
            return (T) Boolean.valueOf(isPreferDirect());
        }
        if (option == JOption.USE_POOLED_ALLOCATOR) {
            return (T) Boolean.valueOf(isUsePooledAllocator());
        }
        return null;
    }

    @Override
    public <T> boolean setOption(JOption<T> option, T value) {
        validate(option, value);

        if (option == JOption.IO_RATIO) {
            setIoRatio((Integer) value);
        } else if (option == JOption.PREFER_DIRECT) {
            setPreferDirect((Boolean) value);
        } else if (option == JOption.USE_POOLED_ALLOCATOR) {
            setUsePooledAllocator((Boolean) value);
        } else {
            return false;
        }
        return true;
    }

    public int getIoRatio() {
        return ioRatio;
    }

    public void setIoRatio(int ioRatio) {
        if (ioRatio < 0) {
            ioRatio = 0;
        }
        if (ioRatio > 100) {
            ioRatio = 100;
        }
        this.ioRatio = ioRatio;
    }

    public boolean isPreferDirect() {
        return preferDirect;
    }

    public void setPreferDirect(boolean preferDirect) {
        this.preferDirect = preferDirect;
    }

    public boolean isUsePooledAllocator() {
        return usePooledAllocator;
    }

    public void setUsePooledAllocator(boolean usePooledAllocator) {
        this.usePooledAllocator = usePooledAllocator;
    }

    protected <T> void validate(JOption<T> option, T value) {
        checkNotNull(option, "option");
        checkNotNull(value, "value");
    }

}
