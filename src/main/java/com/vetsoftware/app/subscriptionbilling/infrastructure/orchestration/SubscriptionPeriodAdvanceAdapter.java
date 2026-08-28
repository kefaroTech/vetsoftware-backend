package com.vetsoftware.app.subscriptionbilling.infrastructure.orchestration;

import com.vetsoftware.app.subscription.application.command.RenewSubscriptionPeriodCommand;
import com.vetsoftware.app.subscription.application.port.in.RenewSubscriptionPeriodUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionPeriodAdvancePort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Cruza a {@code subscription} <b>por su puerto de entrada</b>, no por su
 * repositorio.
 *
 * <p>
 * Mismo patron que {@code DunningReevaluationAdapter}: la unica clase de esta
 * rodaja que conoce a la otra es este adaptador, y lo que consume de ella es su
 * contrato publico —con su {@code @PreAuthorize} y su transaccion— y no sus
 * tablas. Escribir {@code current_period_start} desde un adaptador de
 * persistencia habria saltado el dominio, que es quien comprueba que el periodo
 * no nazca invertido.
 */
@Component
public class SubscriptionPeriodAdvanceAdapter implements SubscriptionPeriodAdvancePort {

    private final RenewSubscriptionPeriodUseCase renewUseCase;

    public SubscriptionPeriodAdvanceAdapter(RenewSubscriptionPeriodUseCase renewUseCase) {
        this.renewUseCase = renewUseCase;
    }

    @Override
    public void advanceTo(Long subscriptionId, Long companyId, LocalDate periodStart,
            LocalDate periodEnd, LocalDate nextBillingDate) {
        renewUseCase.execute(new RenewSubscriptionPeriodCommand(subscriptionId, companyId,
                periodStart, periodEnd, nextBillingDate));
    }
}
