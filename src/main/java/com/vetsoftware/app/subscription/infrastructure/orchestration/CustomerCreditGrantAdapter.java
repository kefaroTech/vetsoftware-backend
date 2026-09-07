package com.vetsoftware.app.subscription.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.customercredit.application.command.GrantCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.port.in.GrantCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.subscription.application.port.out.CustomerCreditGrantPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Origen {@code CANCELLATION}: «baja con periodo pagado por delante», que es
 * exactamente lo que sustituir un contrato ya cobrado le deja al cliente.
 */
@Component
public class CustomerCreditGrantAdapter implements CustomerCreditGrantPort {

    private final GrantCustomerCreditUseCase grantUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public CustomerCreditGrantAdapter(GrantCustomerCreditUseCase grantUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.grantUseCase = grantUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public Long grantForUnusedPeriod(Long companyId, Long subscriptionId, BigDecimal amount,
            LocalDate today, String clientRequestId) {
        return systemAuthRunner.call(() -> grantUseCase.execute(
                new GrantCustomerCreditCommand(companyId, amount, CreditOriginKind.CANCELLATION,
                        null, null, subscriptionId, null, clientRequestId)))
                .id();
    }
}
