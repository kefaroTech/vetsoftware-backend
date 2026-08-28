package com.vetsoftware.app.paymentrefund.application.usecase;

import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.in.FindPaymentRefundUseCase;
import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.paymentrefund.domain.PaymentRefundNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.refund.find")
@Service
public class FindPaymentRefundService implements FindPaymentRefundUseCase {

    private final PaymentRefundRepository repository;

    public FindPaymentRefundService(PaymentRefundRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentRefundDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(PaymentRefundDto::from)
                .orElseThrow(() -> new PaymentRefundNotFoundException(id));
    }
}
