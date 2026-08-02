package com.vetsoftware.app.rolepermission.infrastructure.web.response;

import java.time.LocalDateTime;

public record RolePermissionResponse(Long id, RoleSummary role, PermissionSummary permission,
        LocalDateTime createdDate, boolean enabled) {
}
