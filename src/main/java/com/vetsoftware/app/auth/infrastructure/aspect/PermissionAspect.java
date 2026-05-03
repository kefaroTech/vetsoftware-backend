package com.vetsoftware.app.auth.infrastructure.aspect;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Pointcut("execution(* com.vetsoftware.app..application.port.in.*.*(..))")
    private void useCasePortInvocation() {}

    @Before("useCasePortInvocation()")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresPermission annotation =
            AnnotatedElementUtils.findMergedAnnotation(method, RequiresPermission.class);
        if (annotation == null) return;

        AuthContext auth = Arrays.stream(joinPoint.getArgs())
            .filter(AuthContext.class::isInstance)
            .map(AuthContext.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Method " + signature
                    + " is annotated @RequiresPermission but has no AuthContext parameter"));

        auth.requireAnyPermission(annotation.value());
    }
}
