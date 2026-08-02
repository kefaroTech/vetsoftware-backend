package com.vetsoftware.app.rolepermission.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SyncRolePermissionsRequest(@NotNull List<Long> permissionIds) {}
