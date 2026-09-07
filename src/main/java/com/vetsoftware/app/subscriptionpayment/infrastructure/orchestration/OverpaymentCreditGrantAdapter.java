package com.vetsoftware.app.subscriptionpayment.infrastructure.orchestration;

import com.vetsoftware.app.customercredit.application.command.GrantCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.port.in.GrantCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import com.vetsoftware.app.subscriptionpayment.application.port.out.OverpaymentCreditGrantPort;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class OverpaymentCreditGrantAdapter implements OverpaymentCreditGrantPort {

    private final GrantCustomerCreditUseCase grantUseCase;

    public OverpaymentCreditGrantAdapter(GrantCustomerCreditUseCase grantUseCase) {
        this.grantUseCase = grantUseCase;
    }

    @Override
    public void grantForOverpayment(Long companyId, Long paymentId, Long documentId,
            BigDecimal amount) {
        grantUseCase.execute(new GrantCustomerCreditCommand(companyId, amount,
                CreditOriginKind.OVERPAYMENT, paymentId, null, null, null,
                "overpayment-" + paymentId + "-" + documentId));
    }
}
