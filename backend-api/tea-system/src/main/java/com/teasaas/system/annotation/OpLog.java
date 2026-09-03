package com.teasaas.system.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解（F-SYS-04）：标注在 Controller/Service 方法上，切面自动落库
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /** 模块：order/purchase/bom/system... */
    String module();

    /** 动作描述 */
    String action();
}
