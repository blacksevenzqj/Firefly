package org.firefly.registry.api.registryserver;

import java.lang.reflect.Constructor;
import java.util.List;
import com.google.common.collect.Lists;
import org.firefly.common.util.ExceptionUtil;
import org.firefly.common.util.Reflects;
import org.firefly.common.util.SystemPropertyUtil;

public interface RegistryServer {

    void startRegistryServer();

    /**
     * 用于创建默认的注册中心实现(jupiter-registry-default), 当不使用jupiter-registry-default时, 不能有显示依赖.
     */
    @SuppressWarnings("unchecked")
    class Default {

        private static final Class<RegistryServer> defaultRegistryClass;
        private static final List<Class<?>[]> allConstructorsParameterTypes;

        static {
            Class<RegistryServer> cls;
            try {
                cls = (Class<RegistryServer>) Class.forName(
                        SystemPropertyUtil.get("firefly.registry.default", "org.firefly.transport.netty.acceptor.DefaultRegistryServerNettyTcpAcceptor"));
            } catch (ClassNotFoundException e) {
                cls = null;
            }
            defaultRegistryClass = cls;

            if (defaultRegistryClass != null) {
                allConstructorsParameterTypes = Lists.newArrayList();
                Constructor<?>[] array = defaultRegistryClass.getDeclaredConstructors();
                for (Constructor<?> c : array) {
                    allConstructorsParameterTypes.add(c.getParameterTypes());
                }
            } else {
                allConstructorsParameterTypes = null;
            }
        }

        public static RegistryServer createRegistryServer(int port, int nWorkers) {
            return newInstance(port, nWorkers);
        }

        private static RegistryServer newInstance(Object... parameters) {
            if (defaultRegistryClass == null || allConstructorsParameterTypes == null) {
                throw new UnsupportedOperationException("unsupported default registry");
            }

            // 根据JLS方法调用的静态分派规则查找最匹配的方法parameterTypes
            Class<?>[] parameterTypes = Reflects.findMatchingParameterTypes(allConstructorsParameterTypes, parameters);
            if (parameterTypes == null) {
                throw new IllegalArgumentException("parameter types");
            }

            try {
                @SuppressWarnings("JavaReflectionMemberAccess")
                Constructor<RegistryServer> c = defaultRegistryClass.getConstructor(parameterTypes);
                c.setAccessible(true);
                return c.newInstance(parameters);
            } catch (Exception e) {
                ExceptionUtil.throwException(e);
            }
            return null; // should never get here
        }
    }
}
