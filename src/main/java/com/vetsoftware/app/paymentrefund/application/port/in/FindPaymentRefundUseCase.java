package com.vetsoftware.app.paymentrefund.application.port.in;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPaymentRefundUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. No existe
     * la variante ancha a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     *
     * <p>
     * El tenant llega aqui porque el documento maestro dice que de este bloque
     * <em>lee</em>: una devolucion es plata suya y tiene derecho a verla.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentRefund.read')"
            + " and @authz.isMyCompany(#companyId))")
    PaymentRefundDto findById(Long id, Long companyId);
}
