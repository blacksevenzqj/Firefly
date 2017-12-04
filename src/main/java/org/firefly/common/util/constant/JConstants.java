package org.firefly.common.util.constant;

import org.firefly.common.util.SystemPropertyUtil;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Formatter;

public final class JConstants {

    /** 换行符 */
    public static final String NEWLINE;
    /** 字符编码 */
    public static final String UTF8_CHARSET = "UTF-8";
    public static final Charset UTF8;
    static {
        String newLine;
        try {
            newLine = new Formatter().format("%n").toString();
        } catch (Exception e) {
            newLine = "\n";
        }
        NEWLINE = newLine;

        Charset charset = null;
        try {
            charset = Charset.forName(UTF8_CHARSET);
        } catch (UnsupportedCharsetException ignored) {}
        UTF8 = charset;
    }

    /** Cpu核心数 */
    public static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    /** 未知应用名称 */
    public static final String UNKNOWN_APP_NAME = "UNKNOWN";
    /** 服务默认组别 */
    public static final String DEFAULT_GROUP = "Firefly";
    /** 服务默认版本号 */
    public static final String DEFAULT_VERSION = "1.0.0";
    /** 默认的调用超时时间为3秒 **/
    public static final long DEFAULT_TIMEOUT =
            SystemPropertyUtil.getInt("firefly.rpc.invoke.timeout", 3 * 1000);
    /** Server链路read空闲检测, 默认60秒, 60秒没读到任何数据会强制关闭连接 */
    public static final int READER_IDLE_TIME_SECONDS =
            SystemPropertyUtil.getInt("firefly.io.reader.idle.time.seconds", 60);
    /** Client链路write空闲检测, 默认30秒, 30秒没有向链路中写入任何数据时Client会主动向Server发送心跳数据包 */
    public static final int WRITER_IDLE_TIME_SECONDS =
            SystemPropertyUtil.getInt("firefly.io.writer.idle.time.seconds", 30);

    /** Load balancer 默认预热时间 **/
    public static final int DEFAULT_WARM_UP =
            SystemPropertyUtil.getInt("firefly.rpc.load-balancer.warm-up", 10 * 60 * 1000);
    /** Load balancer 默认权重 **/
    public static final int DEFAULT_WEIGHT =
            SystemPropertyUtil.getInt("firefly.rpc.load-balancer.default.weight", 50);
    /** Load balancer 最大权重 **/
    public static final int MAX_WEIGHT =
            SystemPropertyUtil.getInt("firefly.rpc.load-balancer.max.weight", 100);

    /** Suggest that the count of connections **/
    public static final int SUGGESTED_CONNECTION_COUNT =
            SystemPropertyUtil.getInt("firefly.rpc.suggest.connection.count", AVAILABLE_PROCESSORS);

    /** Metrics csv reporter */
    public static final boolean METRIC_CSV_REPORTER =
            SystemPropertyUtil.getBoolean("firefly.metric.csv.reporter", false);
    /** Metrics csv reporter directory */
    public static final String METRIC_CSV_REPORTER_DIRECTORY =
            SystemPropertyUtil.get("firefly.metric.csv.reporter.directory", SystemPropertyUtil.get("user.dir"));
    /** Metrics reporter period */
    public static final int METRIC_REPORT_PERIOD =
            SystemPropertyUtil.getInt("firefly.metric.report.period", 15);
}
