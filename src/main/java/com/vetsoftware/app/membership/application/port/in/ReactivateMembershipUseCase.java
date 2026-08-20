package com.vetsoftware.app.membership.application.port.in;

import com.vetsoftware.app.membership.application.dto.MembershipDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Alineado con sus hermanos —{@code CreateMembershipUseCase},
 * {@code UpdateMembershipUseCase}, {@code DeleteMembershipUseCase} y
 * {@code FindMembershipUseCase} son {@code hasRole('SYSTEM')} a secas—, que es
 * lo que este puerto debio ser siempre: llevaba
 * {@code hasAuthority('membership.update')} por copia del patron CRUD del
 * tenant, y ese disyunto era una mina armada. No abria nada hoy —la authority
 * no esta sembrada—, pero el dia que alguien la creara le entregaria este
 * catalogo maestro a un administrador de empresa. Quien no puede crear, editar
 * ni desactivar una fila tampoco tiene por que reactivarla: de hecho no podia
 * llegar a ese estado, porque desactivarla ya era SYSTEM.
 */
public interface ReactivateMembershipUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    MembershipDto execute(Long id);
}
