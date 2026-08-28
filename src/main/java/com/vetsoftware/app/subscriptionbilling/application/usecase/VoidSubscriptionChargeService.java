package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.VoidSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionChargeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anula un cargo <b>creando el que lo compensa</b>.
 *
 * <p>
 * Los dos {@code save} van en la misma transacción: si solo entrara uno, o
 * quedaría un cargo marcado {@code VOIDED} sin la fila que lo compensa —y el
 * devengado del periodo dejaría de cerrar— o quedaría una compensación colgando
 * de un cargo que sigue vivo, y el periodo se cobraría en negativo.
 *
 * <p>
 * El original <b>no se borra ni se desactiva</b>: {@code subscription_charges}
 * no lleva {@code enabled} justamente para que ninguna consulta pueda esconder
 * la mitad de la conciliación.
 */
@Observed(name = "subscription.billing.charge.void")
@Service
public class VoidSubscriptionChargeService implements VoidSubscriptionChargeUseCase {

    private final SubscriptionChargeRepository repository;
    private final SubscriptionBillingMetrics metrics;
    private final SubscriptionBillingAuditPort audit;
    private final Clock clock;

    public VoidSubscriptionChargeService(SubscriptionChargeRepository repository,
            SubscriptionBillingMetrics metrics, SubscriptionBillingAuditPort audit, Clock clock) {
        this.repository = repository;
        this.metrics = metrics;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionChargeDto execute(VoidSubscriptionChargeCommand command) {
        SubscriptionCharge original = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionChargeNotFoundException(command.id()));
        SubscriptionCharge compensacion = SubscriptionCharge.voidingOf(original,
                command.description(), clock);
        repository.save(original);
        SubscriptionCharge saved = repository.save(compensacion);

        // Los dos ids salen juntos: el rastro sin el par no permite reconstruir el
        // saldo, porque aqui el dinero no se corrige encima, se compensa.
        metrics.chargeVoided(original.getChargeType());
        audit.chargeVoided(original.getId(), saved.getId(), original.getSubscriptionId(),
                saved.getSubtotalAmount());
        return SubscriptionChargeDto.from(saved);
    }
}
