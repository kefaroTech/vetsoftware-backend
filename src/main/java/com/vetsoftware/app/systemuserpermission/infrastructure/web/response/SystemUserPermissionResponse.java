package com.vetsoftware.app.systemuserpermission.infrastructure.web.response;

import java.time.LocalDateTime;

public record SystemUserPermissionResponse(Long id,
                                           SystemUserSummary systemUser,
                                           SystemPermissionSummary systemPermission,
                                           LocalDateTime createdDate) {}
