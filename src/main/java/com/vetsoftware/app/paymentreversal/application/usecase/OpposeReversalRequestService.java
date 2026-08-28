package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.command.OpposeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.OpposeReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "payment.reversal.oppose")
@Service
public class OpposeReversalRequestService implements OpposeReversalRequestUseCase {

    private final PaymentReversalRequestRepository repository;
    private final Clock clock;

    public OpposeReversalRequestService(PaymentReversalRequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaymentReversalRequestDto execute(OpposeReversalRequestCommand command) {
        PaymentReversalRequest request = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new PaymentReversalRequestNotFoundException(command.id()));
        request.oppose(command.ground(), command.oppositionEvidenceRef(), LocalDateTime.now(clock));
        return PaymentReversalRequestDto.from(repository.save(request));
    }
}
