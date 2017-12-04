package org.firefly.test.serialization;

import org.firefly.serialization.Serializer;
import org.firefly.serialization.SerializerFactory;
import org.firefly.serialization.SerializerType;
import java.io.Serializable;
import java.util.ArrayList;

public class ProtoStuffSerializerTest {

    public static void main(String[] args){
        Serializer serializer = SerializerFactory.getSerializer(SerializerType.PROTO_STUFF.value());
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
