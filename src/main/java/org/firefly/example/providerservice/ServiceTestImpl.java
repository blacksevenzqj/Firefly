package org.firefly.example.providerservice;

import org.firefly.common.util.internal.Lists;
import org.firefly.rpc.provider.annotation.ServiceProviderImpl;

import java.util.Collections;

@ServiceProviderImpl(version = "1.0.0.daily")
public class ServiceTestImpl extends BaseService implements ServiceTest {

    private String strValue;

    public String getStrValue() {
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    @Override
    public ResultClass sayHello(String... s) {
        ResultClass result = new ResultClass();
        result.lon = 1L;
        Integer i = getIntValue();
        result.num = (i == null ? 0 : i);
        result.str = strValue;
        result.list = Lists.newArrayList("H", "e", "l", "l", "o");
        Collections.addAll(result.list, s);
        return result;
    }
}
