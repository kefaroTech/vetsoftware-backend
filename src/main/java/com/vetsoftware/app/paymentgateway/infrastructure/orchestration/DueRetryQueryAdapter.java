package com.vetsoftware.app.paymentgateway.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentattempt.application.port.in.ListDuePaymentAttemptsUseCase;
import com.vetsoftware.app.paymentgateway.application.port.out.DueRetryQueryPort;
import com.vetsoftware.app.paymentgateway.domain.DueRetryTarget;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Delega en {@code paymentattempt} la cola de reintentos vencidos.
 *
 * <p>
 * <strong>Con {@code SystemAuthRunner}</strong>:
 * {@code ListDuePaymentAttemptsUseCase} está cerrado a
 * {@code hasRole('SYSTEM')} a secas —es uno de los barridos de plataforma sin
 * empresa delante— y quien llega hasta aquí ya está bajo esa escalada desde
 * {@code PaymentCollectionJob}.
 */
@Component
public class DueRetryQueryAdapter implements DueRetryQueryPort {

    private final ListDuePaymentAttemptsUseCase listDuePaymentAttemptsUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public DueRetryQueryAdapter(ListDuePaymentAttemptsUseCase listDuePaymentAttemptsUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.listDuePaymentAttemptsUseCase = listDuePaymentAttemptsUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public PageResult<DueRetryTarget> listDue(LocalDateTime now, int page, int pageSize) {
        return systemAuthRunner
                .call(() -> listDuePaymentAttemptsUseCase.listDue(now, page, pageSize))
                .map(dto -> new DueRetryTarget(dto.id(), dto.companyId(), dto.billingDocumentId()));
    }
}
