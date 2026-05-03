package com.vetsoftware.app.systempermission.application.port.in;

import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSystemPermissionsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<SystemPermissionDto> listAll();
}
