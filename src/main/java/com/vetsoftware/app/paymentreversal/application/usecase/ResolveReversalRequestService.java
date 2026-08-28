package com.vetsoftware.app.paymentreversal.application.usecase;

import com.vetsoftware.app.paymentreversal.application.command.ResolveReversalRequestCommand;
import com.vetsoftware.app.paymentreversal.application.dto.PaymentReversalRequestDto;
import com.vetsoftware.app.paymentreversal.application.port.in.ResolveReversalRequestUseCase;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentRefundValidationPort;
import com.vetsoftware.app.paymentreversal.application.port.out.PaymentReversalRequestRepository;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequest;
import com.vetsoftware.app.paymentreversal.domain.PaymentReversalRequestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra el expediente.
 *
 * <p>
 * La devolucion enlazada se valida <strong>acotada por empresa</strong> antes
 * de guardarse: {@code fk_prr_refund} apunta al par {@code (company_id, id)} de
 * {@code payment_refunds}, asi que sin la comprobacion previa un id ajeno
 * saldria como error de integridad ilegible en vez de decir que esa devolucion
 * no es suya.
 */
@Observed(name = "payment.reversal.resolve")
@Service
public class ResolveReversalRequestService implements ResolveReversalRequestUseCase {

    private final PaymentReversalRequestRepository repository;
    private final PaymentRefundValidationPort refundValidationPort;

    public ResolveReversalRequestService(PaymentReversalRequestRepository repository,
            PaymentRefundValidationPort refundValidationPort) {
        this.repository = repository;
        this.refundValidationPort = refundValidationPort;
    }

    @Override
    @Transactional
    public PaymentReversalRequestDto execute(ResolveReversalRequestCommand command) {
        PaymentReversalRequest request = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new PaymentReversalRequestNotFoundException(command.id()));

        if (command.resultingRefundId() != null && !refundValidationPort
                .existsByIdAndCompanyId(command.resultingRefundId(), command.companyId()))
            throw new IllegalArgumentException(
                    "Payment refund not found: " + command.resultingRefundId());

        request.resolve(command.outcome(), command.appliedAmount(), command.resultingRefundId());
        return PaymentReversalRequestDto.from(repository.save(request));
    }
}
