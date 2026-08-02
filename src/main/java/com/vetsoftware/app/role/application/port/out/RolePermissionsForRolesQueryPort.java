package com.vetsoftware.app.role.application.port.out;

import com.vetsoftware.app.role.application.dto.PermissionSummaryDto;
import java.util.List;
import java.util.Map;

public interface RolePermissionsForRolesQueryPort {
  Map<Long, List<PermissionSummaryDto>> findByRoleIds(List<Long> roleIds);
}
