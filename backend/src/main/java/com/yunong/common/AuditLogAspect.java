package com.yunong.common;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 操作审计切面 —— 记录关键操作的操作人、操作类型、目标、耗时
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Around("@annotation(com.yunong.common.AuditLog)")
    public Object around(ProceedingJoinPoint jp) throws Throwable {
        var signature = (MethodSignature) jp.getSignature();
        var annotation = signature.getMethod().getAnnotation(AuditLog.class);

        String operator = "anonymous";
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            operator = auth.getName();
        }

        Instant start = Instant.now();
        try {
            Object result = jp.proceed();
            long ms = Duration.between(start, Instant.now()).toMillis();
            log.warn("AUDIT | {} | {} | {} | {}ms | SUCCESS",
                    operator, annotation.action(), jp.getSignature().toShortString(), ms);
            return result;
        } catch (Exception e) {
            long ms = Duration.between(start, Instant.now()).toMillis();
            log.warn("AUDIT | {} | {} | {} | {}ms | FAILED: {}",
                    operator, annotation.action(), jp.getSignature().toShortString(), ms, e.getMessage());
            throw e;
        }
    }
}
