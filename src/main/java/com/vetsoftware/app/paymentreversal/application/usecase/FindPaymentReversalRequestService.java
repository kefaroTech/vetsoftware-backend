package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.FindPaymentReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "payment.reversal.find")
@Service
public class FindPaymentReversalRequestService implements FindPaymentReversalRequestUseCase {

    private final PaymentReversalRequestRepository repository;

    public FindPaymentReversalRequestService(PaymentReversalRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaymentReversalRequestDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(PaymentReversalRequestDto::from)
                .orElseThrow(() -> new PaymentReversalRequestNotFoundException(id));
    }
}
