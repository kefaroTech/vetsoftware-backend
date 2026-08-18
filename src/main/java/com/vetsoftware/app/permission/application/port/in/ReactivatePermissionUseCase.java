package com.vetsoftware.app.permission.application.port.in;

import com.vetsoftware.app.permission.application.dto.PermissionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alineado con sus tres hermanos —{@code CreatePermissionUseCase},
 * {@code UpdatePermissionUseCase} y {@code DeletePermissionUseCase} son
 * {@code hasRole('SYSTEM')} a secas—, que es lo que este puerto debio ser
 * siempre: llevaba {@code hasAuthority('permission.update')} por copia del
 * patron CRUD del tenant, y ese disyunto era el hueco por el que un
 * administrador de empresa alcanzaba el catalogo de permisos de otra. Quien no
 * puede crear, editar ni desactivar un permiso tampoco tiene por que
 * reactivarlo — de hecho no podia llegar a ese estado, porque desactivarlo ya
 * era SYSTEM.
 *
 * <p>
 * El {@code companyId} se conserva en la firma como defensa en profundidad: un
 * SYSTEM puro lo pasa nulo y opera global, mientras que un principal con
 * empresa seleccionada queda acotado por el {@code AND company_id} del UPDATE.
 * El gate no lo referencia a proposito — con solo {@code hasRole('SYSTEM')} el
 * conjunto del tenant seria codigo muerto.
 */
public interface ReactivatePermissionUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PermissionDto execute(Long id, Long companyId);
}
