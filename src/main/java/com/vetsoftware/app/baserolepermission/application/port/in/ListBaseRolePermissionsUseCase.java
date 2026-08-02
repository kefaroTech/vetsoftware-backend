package com.vetsoftware.app.baserolepermission.application.port.in;

import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListBaseRolePermissionsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<BaseRolePermissionDto> listAll();
}
