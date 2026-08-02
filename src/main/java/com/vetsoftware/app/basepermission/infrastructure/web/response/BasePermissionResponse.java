package com.vetsoftware.app.basepermission.infrastructure.web.response;

import java.time.LocalDateTime;

public record BasePermissionResponse(
    Long id,
    String name,
    String code,
    SubModuleSummary subModule,
    LocalDateTime createdDate,
    boolean enabled) {}
