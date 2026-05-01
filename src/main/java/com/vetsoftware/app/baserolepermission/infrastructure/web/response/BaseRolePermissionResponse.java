package com.vetsoftware.app.baserolepermission.infrastructure.web.response;

import java.time.LocalDateTime;

public record BaseRolePermissionResponse(Long id, Long baseRoleId, Long basePermissionId, LocalDateTime createdDate) {}
