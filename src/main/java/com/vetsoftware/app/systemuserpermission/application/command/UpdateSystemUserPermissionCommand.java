package com.vetsoftware.app.systemuserpermission.application.command;

public record UpdateSystemUserPermissionCommand(
    Long id, Long systemUserId, Long systemPermissionId) {}
