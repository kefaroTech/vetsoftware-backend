package com.vetsoftware.app.role.infrastructure.web.response;

import java.time.LocalDateTime;
import java.util.List;

public record RoleResponse(Long id, String name, String code,
                           CompanySummary company,
                           LocalDateTime createdDate,
                           List<PermissionSummary> permissions) {}
