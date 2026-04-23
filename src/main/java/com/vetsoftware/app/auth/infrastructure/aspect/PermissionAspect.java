package com.vetsoftware.app.auth.infrastructure.aspect;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        Arrays.stream(joinPoint.getArgs())
                .filter(arg -> arg instanceof AuthContext)
                .map(arg -> (AuthContext) arg)
                .findFirst()
                .ifPresent(auth -> auth.requirePermission(requiresPermission.value()));
    }
}
