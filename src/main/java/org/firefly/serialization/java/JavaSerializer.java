package org.firefly.serialization.java;


import org.firefly.common.util.ExceptionUtil;
import org.firefly.common.util.internal.InternalThreadLocal;
import org.firefly.common.util.internal.UnsafeReferenceFieldUpdater;
import org.firefly.common.util.internal.UnsafeUpdater;
import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerType;

import java.io.*;

/**
 * Java自身的序列化/反序列化实现.
 */
public class JavaSerializer extends Serializer {

    private static final UnsafeReferenceFieldUpdater<ByteArrayOutputStream, byte[]> bufUpdater =
            UnsafeUpdater.newReferenceFieldUpdater(ByteArrayOutputStream.class, "buf");

    // 目的是复用 ByteArrayOutputStream 中的 byte[]
    private static final InternalThreadLocal<ByteArrayOutputStream> bufThreadLocal = new InternalThreadLocal<ByteArrayOutputStream>() {

        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(DEFAULT_BUF_SIZE);
        }
    };

    @Override
    public byte code() {
        return SerializerType.JAVA.value();
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        ByteArrayOutputStream buf = bufThreadLocal.get();
        ObjectOutputStream output = null;
        try {
            output = new ObjectOutputStream(buf);
            output.writeObject(obj);
            output.flush();
            return buf.toByteArray();
        } catch (IOException e) {
            ExceptionUtil.throwException(e);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {}
            }

            buf.reset(); // for reuse

            // 防止hold过大的内存块一直不释放
            assert bufUpdater != null;
            if (bufUpdater.get(buf).length > MAX_CACHED_BUF_SIZE) {
                bufUpdater.set(buf, new byte[DEFAULT_BUF_SIZE]);
            }
        }
        return null; // never get here
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(new ByteArrayInputStream(bytes, offset, length));
            Object obj = input.readObject();
            return clazz.cast(obj);
        } catch (Exception e) {
            ExceptionUtil.throwException(e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {}
            }
        }
        return null; // never get here
    }

    @Override
    public String toString() {
        return "java:(code=" + code() + ")";
    }
}
