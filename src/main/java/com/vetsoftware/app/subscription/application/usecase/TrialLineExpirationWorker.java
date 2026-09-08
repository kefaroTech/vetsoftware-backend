package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.companytrialgrant.application.command.ConsumeTrialGrantCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ConsumeTrialGrantUseCase;
import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.port.in.RecalculateCompanyEntitlementsUseCase;
import com.vetsoftware.app.subscription.application.port.out.TrialLineSuccessionPort;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Una transacción por empresa, y la concesión se consume aunque la línea ya la
 * hubiera cerrado una compra a mitad de prueba: quien la cerró no la selló, y
 * el desenlace {@code CONVERTED} sale de su propia política.
 */
@Observed(name = "subscription.trial.line.expire")
@Service
public class TrialLineExpirationWorker {

    private static final Logger log = LoggerFactory.getLogger(TrialLineExpirationWorker.class);

    private final TrialLineSuccessionPort successionPort;
    private final ConsumeTrialGrantUseCase consumeTrialGrant;
    private final RecalculateCompanyEntitlementsUseCase recalculateEntitlements;

    public TrialLineExpirationWorker(TrialLineSuccessionPort successionPort,
            ConsumeTrialGrantUseCase consumeTrialGrant,
            RecalculateCompanyEntitlementsUseCase recalculateEntitlements) {
        this.successionPort = successionPort;
        this.consumeTrialGrant = consumeTrialGrant;
        this.recalculateEntitlements = recalculateEntitlements;
    }

    @Transactional
    public int processCompany(Long companyId, List<CompanyTrialGrantDto> expiredGrants) {
        int transitioned = 0;
        for (CompanyTrialGrantDto grant : expiredGrants) {
            String successorChargeMode = grant.policyTrialOutcome().chargeMode().name();
            if (successionPort.transitionIfOpen(companyId, grant.catalogItemId(),
                    grant.trialEndDate(), successorChargeMode)) {
                transitioned++;
            }
            consumeTrialGrant
                    .execute(new ConsumeTrialGrantCommand(companyId, grant.catalogItemId(), null));
        }
        recalculateEntitlements.execute(new RecalculateCompanyEntitlementsCommand(companyId));
        log.info(
                "Vencimiento de prueba en empresa {}: {} concesion(es) evaluada(s),"
                        + " {} linea(s) sucedida(s)",
                companyId, expiredGrants.size(), transitioned);
        return transitioned;
    }
}
