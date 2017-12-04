package org.firefly.model.rpc.response;

import org.firefly.model.transport.configuration.Status;

/**
 * Provider's response data.
 * 响应信息载体.
 */
public class JResponse {

    private final JResponseBytes responseBytes; // 响应bytes
    private ResultWrapper result;               // 响应对象

    public JResponse(long id) {
        responseBytes = new JResponseBytes(id);
    }

    public JResponse(JResponseBytes responseBytes) {
        this.responseBytes = responseBytes;
    }

    public JResponseBytes responseBytes() {
        return responseBytes;
    }

    public long id() {
        return responseBytes.id();
    }

    public byte status() {
        return responseBytes.status();
    }

    public void status(byte status) {
        responseBytes.status(status);
    }

    public void status(Status status) {
        responseBytes.status(status.value());
    }

    public byte serializerCode() {
        return responseBytes.serializerCode();
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        responseBytes.bytes(serializerCode, bytes);
    }

    public ResultWrapper result() {
        return result;
    }

    public void result(ResultWrapper result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "JResponse{" +
                "status=" + Status.parse(status()) +
                ", id=" + id() +
                ", result=" + result +
                '}';
    }
}
