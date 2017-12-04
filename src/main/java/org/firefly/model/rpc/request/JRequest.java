package org.firefly.model.rpc.request;

/**
 * Consumer's request data.
 *
 * 请求信息载体.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class JRequest {

    private final JRequestBytes requestBytes;   // 请求bytes
    private MessageWrapper message;             // 请求对象

    public JRequest() {
        this(new JRequestBytes());
    }

    public JRequest(JRequestBytes requestBytes) {
        this.requestBytes = requestBytes;
    }

    public JRequestBytes requestBytes() {
        return requestBytes;
    }

    public long invokeId() {
        return requestBytes.invokeId();
    }

    public long timestamp() {
        return requestBytes.timestamp();
    }

    public byte serializerCode() {
        return requestBytes.serializerCode();
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        requestBytes.bytes(serializerCode, bytes);
    }

    public MessageWrapper message() {
        return message;
    }

    public void message(MessageWrapper message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "JRequest{" +
                "invokeId=" + invokeId() +
                ", timestamp=" + timestamp() +
                ", serializerCode=" + serializerCode() +
                ", message=" + message +
                '}';
    }
}
