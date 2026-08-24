package com.vetsoftware.app.entitlement.application.port.in;

import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Reconstruir los permisos y contadores de una empresa desde su contrato.
 *
 * <p>
 * <strong>Hay que llamarlo ante cualquier cambio del contrato</strong>, y la
 * lista es exhaustiva a proposito (R11): alta de contrato, alta o baja de
 * linea, cambio de cantidad, cambio de estado --incluidos el paso a
 * {@code PAST_DUE} y a {@code READ_ONLY}--, fin del periodo de prueba,
 * reactivacion tras pago y cancelacion efectiva. Se llama <em>dentro</em> de la
 * misma transaccion que escribe el contrato: si el recalculo falla, el cambio
 * de contrato tampoco ocurre.
 *
 * <p>
 * Es idempotente: borra fisicamente las filas de la empresa y las reinserta.
 * Ejecutarlo dos veces seguidas produce exactamente el mismo estado.
 */
public interface RecalculateCompanyEntitlementsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    EntitlementRecalculationDto execute(RecalculateCompanyEntitlementsCommand command);
}
