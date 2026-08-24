package com.vetsoftware.app.subscriptionpayment.infrastructure.orchestration;

import com.vetsoftware.app.dunning.application.port.in.EvaluateDunningUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import org.springframework.stereotype.Component;

@Component
public class DunningReevaluationAdapter implements DunningReevaluationPort {

    private final EvaluateDunningUseCase evaluateDunningUseCase;

    public DunningReevaluationAdapter(EvaluateDunningUseCase evaluateDunningUseCase) {
        this.evaluateDunningUseCase = evaluateDunningUseCase;
    }

    @Override
    public void reevaluate(Long billingDocumentId, Long companyId) {
        evaluateDunningUseCase.evaluate(billingDocumentId, companyId);
    }
}
