package com.vetsoftware.app.paymentrefund.application.usecase;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.in.ListPaymentRefundsUseCase;
import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.refund.list.by.company")
@Service
public class ListPaymentRefundsService implements ListPaymentRefundsUseCase {

    private final PaymentRefundRepository repository;

    public ListPaymentRefundsService(PaymentRefundRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<PaymentRefundDto> listByCompany(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize).map(PaymentRefundDto::from);
    }
}
