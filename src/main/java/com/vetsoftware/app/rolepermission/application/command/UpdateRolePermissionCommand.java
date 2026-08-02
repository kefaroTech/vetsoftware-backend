package com.vetsoftware.app.rolepermission.application.command;

public record UpdateRolePermissionCommand(
    Long id, Long roleId, Long permissionId, Long companyId) {}
