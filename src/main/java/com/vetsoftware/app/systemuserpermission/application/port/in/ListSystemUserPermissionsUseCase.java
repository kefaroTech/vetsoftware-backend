package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSystemUserPermissionsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<SystemUserPermissionDto> listAll();
}
