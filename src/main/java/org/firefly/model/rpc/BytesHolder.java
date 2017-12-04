package org.firefly.model.rpc;

/**
 * 消息体bytes载体, 避免在IO线程中序列化/反序列化, 这一层不关注消息体的对象结构.
 *
 * 框架内只有一种情况下会导致不同线程对 {@code BytesHolder} 读/写, 就是transport层decoder会将数据写进
 * {@code BytesHolder} 封装到 {@code Runnable} 中并提交到 {@code Executor}, 但这并不会导致内存一致性相关问题,
 * 因为线程将 {@code Runnable} 对象提交到 {@code Executor} 之前的操作 happen-before 其执行开始;
 *
 * Memory consistency effects:
 *    Actions in a thread prior to submitting a {@code Runnable} object({@code BytesHolder} object in it)
 *    to an {@code Executor} happen-before its execution begins, perhaps in another thread.
 */
public abstract class BytesHolder {

    private byte serializerCode;
    private byte[] bytes;

    public byte serializerCode() {
        return serializerCode;
    }

    public byte[] bytes() {
        return bytes;
    }

    public void bytes(byte serializerCode, byte[] bytes) {
        this.serializerCode = serializerCode;
        this.bytes = bytes;
    }

    public void nullBytes() {
        bytes = null; // help gc
    }

    public int size() {
        return bytes == null ? 0 : bytes.length;
    }
}
