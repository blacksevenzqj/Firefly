package org.firefly.example.providerservice;

import org.firefly.rpc.provider.annotation.ServiceProvider;

import java.io.Serializable;
import java.util.List;

@ServiceProvider(group = "test")
public interface ServiceTest {

    ResultClass sayHello(String... s);

    class ResultClass implements Serializable {
        private static final long serialVersionUID = -6514302641628274984L;

        String str;
        int num;
        Long lon;
        List<String> list;

        @Override
        public String toString() {
            return "ResultClass{" +
                    "str='" + str + '\'' +
                    ", num=" + num +
                    ", lon=" + lon +
                    ", list=" + list +
                    '}';
        }
    }
}
