package com.vetsoftware.app.baserole.application.port.in;

import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alineado con sus hermanos —{@code CreateBaseRoleUseCase},
 * {@code UpdateBaseRoleUseCase}, {@code DeleteBaseRoleUseCase} y
 * {@code FindBaseRoleUseCase} son {@code hasRole('SYSTEM')} a secas—, que es lo
 * que este puerto debio ser siempre: llevaba
 * {@code hasAuthority('base_role.update')} por copia del patron CRUD del
 * tenant, y ese disyunto era una mina armada. No abria nada hoy —la authority
 * no esta sembrada—, pero el dia que alguien la creara le entregaria este
 * catalogo maestro a un administrador de empresa. Quien no puede crear, editar
 * ni desactivar una fila tampoco tiene por que reactivarla: de hecho no podia
 * llegar a ese estado, porque desactivarla ya era SYSTEM.
 */
public interface ReactivateBaseRoleUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    BaseRoleDto execute(Long id);
}
