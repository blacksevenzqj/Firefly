package org.firefly.rpc.provider.processor;

import org.firefly.model.rpc.metadata.ServiceWrapper;
import org.firefly.model.rpc.request.JRequest;
import org.firefly.model.rpc.request.JRequestBytes;
import org.firefly.model.transport.channel.interfice.JChannel;
import org.firefly.model.transport.metadata.Directory;
import org.firefly.rpc.provider.processor.task.ProviderMessageTask;
import org.firefly.rpc.provider.server.JServer;

import java.util.concurrent.Executor;

public class DefaultProviderProcessor extends AbstractProviderProcessor {

    private final JServer server;
    private final Executor executor;

    public DefaultProviderProcessor(JServer server) {
        this(server, ProviderExecutors.executor());
    }

    public DefaultProviderProcessor(JServer server, Executor executor) {
        this.server = server;
        this.executor = executor;
    }

    @Override
    public void handleRequest(JChannel channel, JRequestBytes requestBytes) throws Exception {
        ProviderMessageTask task = new ProviderMessageTask(this, channel, new JRequest(requestBytes));
        if (executor == null) {
            task.run();
        } else {
            executor.execute(task);
        }
    }

    @Override
    public ServiceWrapper lookupService(Directory directory) {
        return server.lookupService(directory);
    }

}
