package com.vetsoftware.app.paymentrefund.application.usecase;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.in.ListPaymentRefundsByPaymentUseCase;
import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.refund.list.by.payment")
@Service
public class ListPaymentRefundsByPaymentService implements ListPaymentRefundsByPaymentUseCase {

    private final PaymentRefundRepository repository;

    public ListPaymentRefundsByPaymentService(PaymentRefundRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentRefundDto> listByPaymentAndCompany(Long paymentId, Long companyId,
            int page, int pageSize) {
        return repository.findAllByCompanyIdAndPaymentId(companyId, paymentId, page, pageSize)
                .map(PaymentRefundDto::from);
    }
}
