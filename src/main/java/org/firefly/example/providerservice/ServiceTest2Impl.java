package org.firefly.example.providerservice;

import org.firefly.rpc.provider.annotation.ServiceProviderImpl;

@ServiceProviderImpl(version = "1.0.0.daily")
public class ServiceTest2Impl implements ServiceTest2 {

    @Override
    public String sayHelloString() {
        return "Hello jupiter";
    }
}
