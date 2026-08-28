package com.vetsoftware.app.paymentreversal.application.port.in;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Consulta cross-tenant de los expedientes para la consola de plataforma.
 *
 * <p>
 * {@code companyId} es un <strong>filtro opcional</strong>, no un ambito:
 * cuando llega vacio la consulta recorre todos los tenants, que es justo lo que
 * este puerto existe para permitir. De ahi {@code hasRole('SYSTEM')} a secas.
 */
public interface ListAllPaymentReversalRequestsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PaymentReversalRequestDto> listAll(Long companyId, int page, int pageSize);
}
