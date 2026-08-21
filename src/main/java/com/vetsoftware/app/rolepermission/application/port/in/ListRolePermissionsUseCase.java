package com.vetsoftware.app.rolepermission.application.port.in;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListRolePermissionsUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes lo abria tambien {@code rolePermissions.read}, que es un permiso de
     * empleado, y este listado no filtra por tenant: cualquier empleado listaba
     * tambien las filas de las demas empresas (BE-29).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<RolePermissionDto> listAll();
}
