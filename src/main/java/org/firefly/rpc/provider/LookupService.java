package org.firefly.rpc.provider;

import org.firefly.model.rpc.metadata.ServiceWrapper;
import org.firefly.model.transport.metadata.Directory;

/**
 * Lookup the service.
 */
public interface LookupService {

    /**
     * Lookup the service by {@link Directory}.
     */
    ServiceWrapper lookupService(Directory directory);
}
