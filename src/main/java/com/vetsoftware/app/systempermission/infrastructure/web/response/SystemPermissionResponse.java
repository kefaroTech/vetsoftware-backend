package com.vetsoftware.app.systempermission.infrastructure.web.response;

import java.time.LocalDateTime;

public record SystemPermissionResponse(
    Long id, String name, String code, LocalDateTime createdDate, boolean enabled) {}
