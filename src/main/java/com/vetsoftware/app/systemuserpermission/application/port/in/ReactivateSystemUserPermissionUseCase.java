package com.vetsoftware.app.systemuserpermission.application.port.in;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Solo {@code ROLE_SYSTEM}, igual que Create/Update/Find/List/Delete de esta
 * feature. Los permisos de un usuario de plataforma no estan acotados a ninguna
 * empresa, asi que aqui no cabe el {@code @authz.isMyCompany(...)} que acompaña
 * a las autoridades por permiso en las features del tenant: una clausula
 * {@code hasAuthority} suelta seria una escalada sin limite de alcance.
 */
public interface ReactivateSystemUserPermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SystemUserPermissionDto execute(Long id);
}
