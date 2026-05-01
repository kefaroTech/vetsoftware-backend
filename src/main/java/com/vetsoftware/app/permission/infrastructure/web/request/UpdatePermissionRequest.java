package com.vetsoftware.app.permission.infrastructure.web.request;

public record UpdatePermissionRequest(String name, String code, Long companyId, Long subModuleId) {}
