package org.firefly.example.providerservice;

import org.firefly.rpc.provider.annotation.ServiceProvider;

@ServiceProvider(group = "test")
public interface ServiceTest2 {
    String sayHelloString();
}
