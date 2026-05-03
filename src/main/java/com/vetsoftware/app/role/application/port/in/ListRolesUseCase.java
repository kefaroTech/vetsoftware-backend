package com.vetsoftware.app.role.application.port.in;

import com.vetsoftware.app.role.application.dto.RoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface ListRolesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<RoleDto> listAll();
}
