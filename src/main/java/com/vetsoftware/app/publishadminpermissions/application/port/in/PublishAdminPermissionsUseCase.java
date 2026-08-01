package com.vetsoftware.app.publishadminpermissions.application.port.in;

import com.vetsoftware.app.publishadminpermissions.application.dto.PublishAdminPermissionsDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PublishAdminPermissionsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PublishAdminPermissionsDto execute();
}
