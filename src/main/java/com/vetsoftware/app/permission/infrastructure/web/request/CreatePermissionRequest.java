package com.vetsoftware.app.permission.infrastructure.web.request;

public record CreatePermissionRequest(String name, String code, Long companyId, Long subModuleId) {}
