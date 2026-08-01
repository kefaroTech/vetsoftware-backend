package com.vetsoftware.app.basepermission.application.port.in;

import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListBasePermissionsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<BasePermissionDto> listAll();
}
