package com.yunong.common;

import java.lang.annotation.*;

/** 标记需要审计的方法 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /** 操作类型 */
    String action();
    /** 操作目标描述（支持 SpEL，如 #id） */
    String target() default "";
}
