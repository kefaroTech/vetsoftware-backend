package com.vetsoftware.app.paymentgateway.application.usecase;

import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodPaymentDto;
import com.vetsoftware.app.paymentgateway.application.port.in.FindFirstPeriodPaymentUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.CurrentContractQueryPort;
import com.vetsoftware.app.paymentgateway.application.port.out.FirstPeriodPaymentQueryPort;
import com.vetsoftware.app.paymentgateway.domain.CurrentContractRef;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentStatus;
import com.vetsoftware.app.paymentgateway.domain.FirstPeriodPaymentSnapshot;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solo lecturas de otras rodajas: sí lleva {@code @Transactional(readOnly =
 * true)}, a diferencia del resto de servicios de este slice, que hacen I/O de
 * pasarela.
 */
@Observed(name = "payment.gateway.first.period.payment.find")
@Service
public class FindFirstPeriodPaymentService implements FindFirstPeriodPaymentUseCase {

    private static final String REFERENCE_PREFIX = "VS-";
    private static final String REFERENCE_SUFFIX = "-P1";

    private final CurrentContractQueryPort currentContractQueryPort;
    private final FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort;

    public FindFirstPeriodPaymentService(CurrentContractQueryPort currentContractQueryPort,
            FirstPeriodPaymentQueryPort firstPeriodPaymentQueryPort) {
        this.currentContractQueryPort = currentContractQueryPort;
        this.firstPeriodPaymentQueryPort = firstPeriodPaymentQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public FirstPeriodPaymentDto execute(Long companyId) {
        CurrentContractRef contract = currentContractQueryPort.findCurrent(companyId).orElse(null);
        if (contract == null) {
            return notAttempted();
        }
        String reference = REFERENCE_PREFIX + contract.subscriptionNumber() + REFERENCE_SUFFIX;
        FirstPeriodPaymentSnapshot snapshot = firstPeriodPaymentQueryPort
                .findByCompanyIdAndReference(companyId, reference).orElse(null);
        if (snapshot == null) {
            return notAttempted();
        }
        FirstPeriodPaymentStatus status = switch (snapshot.status()) {
            case "CONFIRMED", "REFUNDED" -> FirstPeriodPaymentStatus.APPROVED;
            case "PENDING" -> FirstPeriodPaymentStatus.PENDING;
            default -> FirstPeriodPaymentStatus.DECLINED;
        };
        return new FirstPeriodPaymentDto(status, snapshot.amount(), snapshot.currency(),
                snapshot.gatewayReference(), snapshot.receivedAt());
    }

    private static FirstPeriodPaymentDto notAttempted() {
        return new FirstPeriodPaymentDto(FirstPeriodPaymentStatus.NOT_ATTEMPTED, null, null, null,
                null);
    }
}
