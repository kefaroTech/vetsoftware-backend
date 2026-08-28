package com.vetsoftware.app.paymentrefund.application.port.in;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPaymentRefundsByPaymentUseCase {

    /**
     * Las devoluciones de un pago concreto: es como se ve cuanto queda por devolver
     * de el, que es la pregunta que se hace antes de devolver otra vez.
     *
     * <p>
     * <strong>Acotar por {@code paymentId} no basta y por eso el {@code companyId}
     * sigue viajando</strong>: una FK ajena no cuenta como filtro de empresa -el
     * pago es de alguien- y ese es el mismo criterio de BE-29 y de la familia «por
     * id». Sin la empresa, escribir el id de un pago vecino en la URL enseñaria sus
     * devoluciones.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('paymentRefund.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<PaymentRefundDto> listByPaymentAndCompany(Long paymentId, Long companyId, int page,
            int pageSize);
}
