package org.firefly.model.registry;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class PublishSubscriptionMessage implements Serializable{

    private static final AtomicLong sequenceGenerator = new AtomicLong(0);

    private final long sequence;
    private final byte serializerCode;
    private byte messageCode;
    private long version; // 版本号
    private Object data;

    public PublishSubscriptionMessage(byte serializerCode) {
        this(sequenceGenerator.getAndIncrement(), serializerCode);
    }

    public PublishSubscriptionMessage(long sequence, byte serializerCode) {
        this.sequence = sequence;
        this.serializerCode = serializerCode;
    }

    public long sequence() {
        return sequence;
    }

    public byte serializerCode() {
        return serializerCode;
    }

    public byte messageCode() {
        return messageCode;
    }

    public void messageCode(byte messageCode) {
        this.messageCode = messageCode;
    }

    public long version() {
        return version;
    }

    public void version(long version) {
        this.version = version;
    }

    public Object data() {
        return data;
    }

    public void data(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Message{" +
                "sequence=" + sequence +
                ", messageCode=" + messageCode +
                ", serializerCode=" + serializerCode +
                ", version=" + version +
                ", data=" + data +
                '}';
    }
}
