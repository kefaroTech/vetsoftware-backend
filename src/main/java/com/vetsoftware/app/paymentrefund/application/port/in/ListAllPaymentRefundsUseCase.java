package com.vetsoftware.app.paymentrefund.application.port.in;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Consulta cross-tenant de tesoreria para la consola de plataforma: que se ha
 * devuelto, a quien y por que motivo, en todas las clinicas.
 */
public interface ListAllPaymentRefundsUseCase {

    /**
     * <strong>{@code hasRole('SYSTEM')} a secas y sin alternativa por
     * permiso.</strong> Devuelve filas de todas las empresas cuando
     * {@code companyId} viene vacio, y abrirlo por {@code hasAuthority} seria
     * exactamente la fuga que {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} persigue.
     *
     * @param companyId
     *            filtro opcional de la consola. Cuando viene, acota; cuando no, el
     *            barrido es completo
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PaymentRefundDto> listAll(Long companyId, int page, int pageSize);
}
