package org.firefly.test.serialization;

import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import org.firefly.serialization.SerializerType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JavaSerializerTest {

    public static void main(String[] args){
        Serializer serializer = SerializerFactory.getSerializer(SerializerType.JAVA.value());
        ResultWrapper wrapper = new ResultWrapper();
        wrapper.setResult("test");
        wrapper.setError(new RuntimeException("test"));
        // Class<?>[] parameterTypes 需要优化 -------- 后续: 已优化掉了
        wrapper.setClazz(new Class[] { String.class, ArrayList.class, Serializable.class });
        byte[] bytes = serializer.writeObject(wrapper);
        ResultWrapper wrapper1 = serializer.readObject(bytes, ResultWrapper.class);
        wrapper1.getError().printStackTrace();
        System.out.println(bytes.length);
        System.out.println(wrapper1.getResult());
        // noinspection ImplicitArrayToString
        System.out.println(wrapper1.getClazz());
        System.out.println(String.valueOf(wrapper1.getResult()));

        SerializerInterface obj = new SerializerObj();
        obj.setStr("SerializerObj1");
        wrapper.setResult(obj);
        bytes = serializer.writeObject(wrapper);
        ResultWrapper wrapper2 = serializer.readObject(bytes, ResultWrapper.class);
        System.out.println(wrapper2.getResult());
        System.out.println(String.valueOf(wrapper2.getResult()));
    }
}

class ResultWrapper implements Serializable {

    private static final long serialVersionUID = -1126932930252953428L;

    private Object result; // 服务调用结果
    private Exception error; // 错误信息
    private Class<?>[] clazz;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public Class<?>[] getClazz() {
        return clazz;
    }

    public void setClazz(Class<?>[] clazz) {
        this.clazz = clazz;
    }

    @Override
    public String toString() {
        return "ResultWrapper{" +
                "result=" + result +
                ", error=" + error +
                '}';
    }
}

interface SerializerInterface extends Serializable {
    String getStr();
    void setStr(String str);
}

class SerializerObj implements SerializerInterface {

    private static final long serialVersionUID = 240893544932658199L;
    String str;
    List<String> list = new ArrayList<>();

    public SerializerObj() { // 反序列化时不会被调用
        list.add("test");
    }

    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return "SerializerObj{" +
                "str='" + str + '\'' +
                ", list=" + list +
                '}';
    }
}

