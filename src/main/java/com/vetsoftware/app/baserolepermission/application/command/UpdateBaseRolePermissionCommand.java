package com.vetsoftware.app.baserolepermission.application.command;

public record UpdateBaseRolePermissionCommand(Long id, Long baseRoleId, Long basePermissionId) {}
