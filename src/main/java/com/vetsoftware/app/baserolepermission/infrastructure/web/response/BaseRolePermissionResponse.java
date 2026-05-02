package com.vetsoftware.app.baserolepermission.infrastructure.web.response;

import java.time.LocalDateTime;

public record BaseRolePermissionResponse(Long id,
                                          BaseRoleSummary baseRole,
                                          BasePermissionSummary basePermission,
                                          LocalDateTime createdDate) {}
