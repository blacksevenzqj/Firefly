package org.firefly.rpc.tracking;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.firefly.common.util.NetUtil;
import org.firefly.common.util.StringBuilderHelper;
import org.firefly.common.util.SystemClock;
import org.firefly.common.util.SystemPropertyUtil;
import org.firefly.common.util.internal.InternalThreadLocal;
import org.firefly.common.util.spi.JServiceLoader;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.firefly.common.util.exception.StackTraceUtil.stackTrace;

/**
 * 链路追踪ID生成的工具类.
 *
 * 一个 {@link TraceId} 包含以下内容(30位):
 * 1  ~ 8  位: 本机IP地址(16进制), 可能是网卡中第一个有效的IP地址
 * 9  ~ 21 位: 当前时间, 毫秒数
 * 22 ~ 25 位: 本地自增ID(1001 ~ 9191 循环使用)
 * 26      位: d (进程flag)
 * 27 ~ 30 位: 当前进程ID(16进制)
 */
public class TracingUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(TracingUtil.class);

    private static final boolean TRACING_NEEDED = SystemPropertyUtil.getBoolean("firefly.tracing.needed", true);

    private static final TracingRecorder tracingRecorder = JServiceLoader.load(TracingRecorder.class).first();

    private static final InternalThreadLocal<TraceId> traceThreadLocal = new InternalThreadLocal<>();

    // maximal value for 64bit systems is 2^22, see man 5 proc.
    private static final int MAX_PROCESS_ID = 4194304;
    private static final char PID_FLAG = 'd';
    private static final String IP_16;
    private static final String PID;
    private static final long ID_BASE = 1000;
    private static final long ID_MASK = (1 << 13) - 1; // 8192 - 1
    private static final AtomicLong sequence = new AtomicLong();

    static {
        String _ip_16;
        try {
            String ip = SystemPropertyUtil.get("firefly.local.address", NetUtil.getLocalAddress());
            _ip_16 = getIP_16(ip);
        } catch (Throwable t) {
            _ip_16 = "ffffffff";
        }
        IP_16 = _ip_16;

        String _pid;
        try {
            _pid = getHexProcessId(getProcessId());
        } catch (Throwable t) {
            _pid = "0000";
        }
        PID = _pid;
    }

    public static boolean isTracingNeeded() {
        return TRACING_NEEDED;
    }

    public static String generateTraceId() {
        return getTraceId(IP_16, SystemClock.millisClock().now(), getNextId());
    }

    public static TraceId getCurrent() {
        TraceId traceId = null;
        if (TRACING_NEEDED) {
            traceId = traceThreadLocal.get();
        }
        return traceId != null ? traceId : TraceId.NULL_TRACE_ID;
    }

    public static void setCurrent(TraceId traceId) {
        if (traceId == null) {
            traceThreadLocal.remove();
        } else {
            traceThreadLocal.set(traceId);
        }
    }

    public static void clearCurrent() {
        traceThreadLocal.remove();
    }

    public static TracingRecorder getRecorder() {
        return tracingRecorder;
    }

    private static String getHexProcessId(int pid) {
        // unsigned short 0 to 65535
        if (pid < 0) {
            pid = 0;
        }
        if (pid > 65535) {
            String strPid = Integer.toString(pid);
            strPid = strPid.substring(strPid.length() - 4, strPid.length());
            pid = Integer.parseInt(strPid);
        }
        StringBuilder buf = new StringBuilder(Integer.toHexString(pid));
        while (buf.length() < 4) {
            buf.insert(0, "0");
        }
        return buf.toString();
    }

    /**
     * Gets current pid, max pid 32 bit systems 32768, for 64 bit 4194304
     * http://unix.stackexchange.com/questions/16883/what-is-the-maximum-value-of-the-pid-of-a-process
     * http://stackoverflow.com/questions/35842/how-can-a-java-program-get-its-own-process-id
     */
    private static int getProcessId() {
        String value = "";
        try {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            value = runtime.getName();
        } catch (Throwable t) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not invoke ManagementFactory.getRuntimeMXBean().getName(), {}.", stackTrace(t));
            }
        }

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        int atIndex = value.indexOf('@');
        if (atIndex >= 0) {
            value = value.substring(0, atIndex);
        }

        int pid = -1;
        try {
            pid = Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // value did not contain an integer
        }

        if (pid < 0 || pid > MAX_PROCESS_ID) {
            pid = ThreadLocalRandom.current().nextInt(MAX_PROCESS_ID + 1);

            logger.warn("Failed to find the current process ID from '{}'; using a random value: {}.",  value, pid);
        }

        return pid;
    }

    private static String getIP_16(String ip) {
        String[] segments = ip.split("\\.");
        StringBuilder buf = StringBuilderHelper.get();
        for (String s : segments) {
            String hex = Integer.toHexString(Integer.parseInt(s));
            if (hex.length() == 1) {
                buf.append('0');
            }
            buf.append(hex);
        }
        return buf.toString();
    }

    private static String getTraceId(String ip_16, long timestamp, long nextId) {
        StringBuilder buf = StringBuilderHelper.get()
                .append(ip_16)
                .append(timestamp)
                .append(nextId)
                .append(PID_FLAG)
                .append(PID);
        return buf.toString();
    }

    private static long getNextId() {
        // (1000 + 1) ~ (1000 + 8191)
        return (sequence.incrementAndGet() & ID_MASK) + ID_BASE;
    }
}
