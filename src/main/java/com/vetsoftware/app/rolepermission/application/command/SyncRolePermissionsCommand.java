package com.vetsoftware.app.rolepermission.application.command;

import java.util.List;

public record SyncRolePermissionsCommand(Long roleId, List<Long> permissionIds, Long companyId) {
}
