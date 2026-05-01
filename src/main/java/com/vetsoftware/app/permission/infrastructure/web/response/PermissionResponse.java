package com.vetsoftware.app.permission.infrastructure.web.response;

import java.time.LocalDateTime;

public record PermissionResponse(Long id, String name, String code, Long companyId, Long subModuleId, LocalDateTime createdDate) {}
