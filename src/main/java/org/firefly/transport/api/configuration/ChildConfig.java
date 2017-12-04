package org.firefly.transport.api.configuration;

import org.firefly.common.util.internal.Lists;
import org.firefly.model.transport.configuration.netty.JOption;
import org.firefly.transport.api.configuration.template.NettyConfig;

import java.util.Collections;
import java.util.List;

import static org.firefly.common.util.Preconditions.checkNotNull;

/**
 * TCP netty child option
 */
public class ChildConfig extends NettyConfig {
    private volatile int rcvBuf = -1;
    private volatile int sndBuf = -1;
    private volatile int linger = -1;
    private volatile int ipTos = -1;
    private volatile int connectTimeoutMillis = -1;
    private volatile int writeBufferHighWaterMark = -1;
    private volatile int writeBufferLowWaterMark = -1;
    private volatile boolean reuseAddress = true;
    private volatile boolean keepAlive = true;
    private volatile boolean tcpNoDelay = true;
    private volatile boolean allowHalfClosure = false;

    @Override
    public List<JOption<?>> getOptions() {
        return getOptions(super.getOptions(),
                JOption.SO_RCVBUF,
                JOption.SO_SNDBUF,
                JOption.SO_LINGER,
                JOption.SO_REUSEADDR,
                JOption.CONNECT_TIMEOUT_MILLIS,
                JOption.WRITE_BUFFER_HIGH_WATER_MARK,
                JOption.WRITE_BUFFER_LOW_WATER_MARK,
                JOption.KEEP_ALIVE,
                JOption.TCP_NODELAY,
                JOption.IP_TOS,
                JOption.ALLOW_HALF_CLOSURE);
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

        if (option == JOption.SO_RCVBUF) {
            return (T) Integer.valueOf(getRcvBuf());
        }
        if (option == JOption.SO_SNDBUF) {
            return (T) Integer.valueOf(getSndBuf());
        }
        if (option == JOption.SO_LINGER) {
            return (T) Integer.valueOf(getLinger());
        }
        if (option == JOption.IP_TOS) {
            return (T) Integer.valueOf(getIpTos());
        }
        if (option == JOption.CONNECT_TIMEOUT_MILLIS) {
            return (T) Integer.valueOf(getConnectTimeoutMillis());
        }
        if (option == JOption.WRITE_BUFFER_HIGH_WATER_MARK) {
            return (T) Integer.valueOf(getWriteBufferHighWaterMark());
        }
        if (option == JOption.WRITE_BUFFER_LOW_WATER_MARK) {
            return (T) Integer.valueOf(getWriteBufferLowWaterMark());
        }
        if (option == JOption.SO_REUSEADDR) {
            return (T) Boolean.valueOf(isReuseAddress());
        }
        if (option == JOption.KEEP_ALIVE) {
            return (T) Boolean.valueOf(isKeepAlive());
        }
        if (option == JOption.TCP_NODELAY) {
            return (T) Boolean.valueOf(isTcpNoDelay());
        }
        if (option == JOption.ALLOW_HALF_CLOSURE) {
            return (T) Boolean.valueOf(isAllowHalfClosure());
        }

        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(JOption<T> option, T value) {
        validate(option, value);

        if (option == JOption.SO_RCVBUF) {
            setRcvBuf((Integer) value);
        } else if (option == JOption.SO_SNDBUF) {
            setSndBuf((Integer) value);
        } else if (option == JOption.SO_LINGER) {
            setLinger((Integer) value);
        } else if (option == JOption.IP_TOS) {
            setIpTos((Integer) value);
        } else if (option == JOption.CONNECT_TIMEOUT_MILLIS) {
            setConnectTimeoutMillis((Integer) value);
        } else if (option == JOption.WRITE_BUFFER_HIGH_WATER_MARK) {
            setWriteBufferHighWaterMark((Integer) value);
        } else if (option == JOption.WRITE_BUFFER_LOW_WATER_MARK) {
            setWriteBufferLowWaterMark((Integer) value);
        } else if (option == JOption.SO_REUSEADDR) {
            setReuseAddress((Boolean) value);
        } else if (option == JOption.KEEP_ALIVE) {
            setKeepAlive((Boolean) value);
        } else if (option == JOption.TCP_NODELAY) {
            setTcpNoDelay((Boolean) value);
        } else if (option == JOption.ALLOW_HALF_CLOSURE) {
            setAllowHalfClosure((Boolean) value);
        } else {
            return super.setOption(option, value);
        }

        return true;
    }

    public int getRcvBuf() {
        return rcvBuf;
    }

    public void setRcvBuf(int rcvBuf) {
        this.rcvBuf = rcvBuf;
    }

    public int getSndBuf() {
        return sndBuf;
    }

    public void setSndBuf(int sndBuf) {
        this.sndBuf = sndBuf;
    }

    public int getLinger() {
        return linger;
    }

    public void setLinger(int linger) {
        this.linger = linger;
    }

    public int getIpTos() {
        return ipTos;
    }

    public void setIpTos(int ipTos) {
        this.ipTos = ipTos;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getWriteBufferHighWaterMark() {
        return writeBufferHighWaterMark;
    }

    public void setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        this.writeBufferHighWaterMark = writeBufferHighWaterMark;
    }

    public int getWriteBufferLowWaterMark() {
        return writeBufferLowWaterMark;
    }

    public void setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        this.writeBufferLowWaterMark = writeBufferLowWaterMark;
    }

    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isAllowHalfClosure() {
        return allowHalfClosure;
    }

    public void setAllowHalfClosure(boolean allowHalfClosure) {
        this.allowHalfClosure = allowHalfClosure;
    }
}
