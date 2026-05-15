package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPermissionsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('rolePermissions.read') or hasRole('SYSTEM')")
    List<PermissionDto> listAll();
}
