package org.firefly.common.util.interfice;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SpiImpl {

    String name() default "";
}
