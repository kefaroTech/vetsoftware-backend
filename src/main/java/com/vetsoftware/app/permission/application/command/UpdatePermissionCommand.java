package com.vetsoftware.app.permission.application.command;

public record UpdatePermissionCommand(Long id, String name, String code, Long companyId,
        Long subModuleId) {
}
