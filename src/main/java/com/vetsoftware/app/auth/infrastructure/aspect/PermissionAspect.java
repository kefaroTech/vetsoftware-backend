package com.vetsoftware.app.auth.infrastructure.aspect;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Before("execution(* com.vetsoftware.app..application.port.in.*.*(..))")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresPermission annotation = findAnnotation(method, joinPoint.getTarget().getClass());
        if (annotation == null) return;

        Arrays.stream(joinPoint.getArgs())
                .filter(AuthContext.class::isInstance)
                .map(AuthContext.class::cast)
                .findFirst()
                .ifPresent(auth -> auth.requireAnyPermission(annotation.value()));
    }

    private RequiresPermission findAnnotation(Method method, Class<?> targetClass) {
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        if (annotation != null) return annotation;

        for (Class<?> iface : targetClass.getInterfaces()) {
            try {
                Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                annotation = ifaceMethod.getAnnotation(RequiresPermission.class);
                if (annotation != null) return annotation;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }
}
