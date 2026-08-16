package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPermissionsUseCase {
    /**
     * Listado GLOBAL, de todas las empresas. Solo {@code ROLE_SYSTEM}.
     *
     * <p>
     * Antes lo abria tambien {@code rolePermissions.read}, que es un permiso de
     * empleado, y este listado no filtra por tenant: cualquier empleado listaba
     * tambien las filas de las demas empresas. Lo que un tenant necesita es
     * {@code GET /permissions/by-company}, que si filtra por empresa (BE-29).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    List<PermissionDto> listAll();
}
