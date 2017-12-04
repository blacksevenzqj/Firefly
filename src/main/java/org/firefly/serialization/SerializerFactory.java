package org.firefly.serialization;

import org.firefly.common.util.collection.ByteObjectHashMap;
import org.firefly.common.util.collection.ByteObjectMap;
import org.firefly.common.util.spi.JServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds all serializers.
 */
public final class SerializerFactory {

    private static final Logger logger = LoggerFactory.getLogger(SerializerFactory.class);

    private static final ByteObjectMap<Serializer> serializers = new ByteObjectHashMap<>();

    static {
        Iterable<Serializer> all = JServiceLoader.load(Serializer.class);
        for (Serializer s : all) {
            serializers.put(s.code(), s);
        }
        logger.info("Supported serializers: {}.", serializers);
    }

    public static Serializer getSerializer(byte code) {
        Serializer serializer = serializers.get(code);

        if (serializer == null) {
            SerializerType type = SerializerType.parse(code);
            if (type != null) {
                throw new IllegalArgumentException("serializer implementation [" + type.name() + "] not found");
            } else {
                throw new IllegalArgumentException("unsupported serializer type with code: " + code);
            }
        }

        return serializer;
    }
}
