package com.vetsoftware.app.permission.application.command;

public record CreatePermissionCommand(String name, String code, Long companyId, Long subModuleId) {}
