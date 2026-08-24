package com.vetsoftware.app.quote.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Baja logica, solo de borradores. Una oferta enviada ya no se puede esconder.
 *
 * <p>
 * <strong>Es de la plataforma, por escrito.</strong> Solo se dan de baja
 * borradores, y los borradores los crea {@link CreateQuoteUseCase}, que es de
 * plataforma: dar de baja aqui es retirar una oferta propia antes de ensenarla,
 * no una operacion que el cliente pueda tener motivo de hacer. Lo que el
 * cliente hace con una oferta que no quiere es rechazarla
 * —{@code RejectQuoteUseCase}, con su rama de tenant—, y eso deja rastro en vez
 * de esconderla.
 *
 * <p>
 * <strong>Consecuencia conocida y aceptada:</strong> mientras esto sea SYSTEM a
 * secas, la sobrecarga acotada {@code QuoteRepository.softDelete(id,
 * companyId)} no tiene ningun llamador por HTTP. {@code DeleteQuoteService} la
 * elige cuando {@code companyId != null} y el controller lo saca de
 * {@code authz.currentCompanyIdOrNull()}, que para todo principal SYSTEM
 * devuelve {@code null}. La sobrecarga se queda porque es el par que
 * {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} exime, no porque se use.
 *
 * <p>
 * Si algun dia se abre al tenant, el {@code #} del SpEL es {@code #companyId} y
 * no {@code #command.companyId}: este puerto recibe dos {@code Long} sueltos,
 * no un command.
 */
public interface DeleteQuoteUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    void execute(Long id, Long companyId);
}
