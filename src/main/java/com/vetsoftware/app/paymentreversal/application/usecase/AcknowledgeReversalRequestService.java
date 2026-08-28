package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.command.AcknowledgeReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.AcknowledgeReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La fecha del acuse la pone el servidor con el reloj inyectado, no quien
 * llama: es una constancia probatoria, y dejar que el cliente elija su fecha
 * vacia exactamente lo que la constancia sirve para demostrar.
 */
@Observed(name = "payment.reversal.acknowledge")
@Service
public class AcknowledgeReversalRequestService implements AcknowledgeReversalRequestUseCase {

    private final PaymentReversalRequestRepository repository;
    private final Clock clock;

    public AcknowledgeReversalRequestService(PaymentReversalRequestRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PaymentReversalRequestDto execute(AcknowledgeReversalRequestCommand command) {
        PaymentReversalRequest request = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new PaymentReversalRequestNotFoundException(command.id()));
        request.acknowledge(command.acknowledgementRef(), LocalDateTime.now(clock));
        return PaymentReversalRequestDto.from(repository.save(request));
    }
}
