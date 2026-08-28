package com.vetsoftware.app.paymentrefund.application.usecase;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.in.ListAllPaymentRefundsUseCase;
import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * El filtro por empresa es opcional porque lo elige la consola de plataforma,
 * no un tenant: el puerto esta cerrado a {@code hasRole('SYSTEM')} y un
 * principal SYSTEM no tiene empresa propia. Con {@code companyId} acota, sin el
 * barre.
 */
@Observed(name = "payment.refund.list.all")
@Service
public class ListAllPaymentRefundsService implements ListAllPaymentRefundsUseCase {

    private final PaymentRefundRepository repository;

    public ListAllPaymentRefundsService(PaymentRefundRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentRefundDto> listAll(Long companyId, int page, int pageSize) {
        PageResult<PaymentRefund> refunds = companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize);
        return refunds.map(PaymentRefundDto::from);
    }
}
