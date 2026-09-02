package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.AccrueOverageChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.AccrueOverageChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.NonBillableSubscriptionItemException;
import com.vetsoftware.app.subscriptionbilling.domain.ProrationBasis;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionItemBillingProfile;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Devenga el cargo por consumo por encima del cupo contratado.
 *
 * <p>
 * <b>Es un service aparte de {@link CreateSubscriptionChargeService} porque es
 * un caso de uso aparte</b> —un service por caso de uso, nunca dos interfaces
 * en uno—, y sobre todo porque su gate es distinto: este lo dispara el empleado
 * que se pasó del cupo, aquél solo la plataforma. El motivo completo está en
 * {@link AccrueOverageChargeUseCase}.
 *
 * <p>
 * <b>Sin {@code @Transactional} propia, y es deliberado.</b> Lo llama
 * {@code OverageChargeAdapter} desde dentro de la transacción de
 * {@code AdjustCompanyCapacityUsageService}, y con propagación {@code REQUIRED}
 * se une a ella: el consumo por encima del techo y el cargo que lo cobra
 * <b>viven o mueren juntos</b>. Abrir aquí una {@code REQUIRES_NEW} —como sí
 * hace la bitácora de portazos, que existe justo para sobrevivir a la vuelta
 * atrás— dejaría el consumo aplicado con el cargo revertido: la clínica se pasa
 * del cupo gratis y no queda ni una fila que lo reclame.
 *
 * <p>
 * <b>Las dos guardas que hace, y por qué esas dos.</b> Resuelve el contrato por
 * la variante acotada por empresa —igual que el alta general, y por lo mismo:
 * sin ella el cargo de la clínica A colgaría del contrato de la B, y la FK
 * compuesta lo cazaría como un 500 de constraint a mitad de una operación
 * clínica—; y exige que la línea <b>cobre</b> (R-TRIAL-14). Una línea en prueba
 * que declara excedente es una configuración contradictoria, y la respuesta
 * correcta es negarse en voz alta —409, con el modo dentro del mensaje— y no
 * regalar el exceso: la excepción sale de aquí, tumba la transacción y con ella
 * el consumo, que es exactamente lo que debe pasar.
 *
 * <p>
 * <b>El impuesto lo pone la línea, no este service.</b> El excedente es más
 * consumo del mismo artículo contratado, así que hereda el
 * {@code tax_treatment} y el {@code tax_rate} de {@code subscription_items} —el
 * mismo origen del que bebe el motor recurrente—, y los trae la misma lectura
 * que ya comprobaba que la línea cobra. Aquí no queda ningún valor fiscal
 * escrito a mano, y es deliberado: cuando lo hubo —{@code EXCLUDED} + tarifa
 * cero fijas— una línea gravada al 19 % devengaba su excedente sin IVA, es
 * decir <b>una factura emitida de menos ante la DIAN</b>, sin ningún síntoma
 * visible hasta la fiscalización.
 *
 * <p>
 * <b>No comprueba que el excedente estuviera permitido</b>: eso ya lo hizo
 * {@code OverageAllowancePort} leyendo {@code subscription_item_limits}, que es
 * quien tiene el dato. Repetirlo aquí invitaría a que un día solo quedara la
 * copia de este lado.
 */
@Observed(name = "subscription.billing.charge.overage")
@Service
public class AccrueOverageChargeService implements AccrueOverageChargeUseCase {

    private final SubscriptionChargeRepository repository;
    private final SubscriptionQueryPort subscriptionQueryPort;
    private final SubscriptionItemValidationPort subscriptionItemValidationPort;
    private final SubscriptionBillingMetrics metrics;
    private final SubscriptionBillingAuditPort audit;
    private final Clock clock;

    public AccrueOverageChargeService(SubscriptionChargeRepository repository,
            SubscriptionQueryPort subscriptionQueryPort,
            SubscriptionItemValidationPort subscriptionItemValidationPort,
            SubscriptionBillingMetrics metrics, SubscriptionBillingAuditPort audit, Clock clock) {
        this.repository = repository;
        this.subscriptionQueryPort = subscriptionQueryPort;
        this.subscriptionItemValidationPort = subscriptionItemValidationPort;
        this.metrics = metrics;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public SubscriptionChargeDto execute(AccrueOverageChargeCommand command) {
        if (command.overageUnits() <= 0)
            throw new IllegalArgumentException("overageUnits must be greater than zero");
        SubscriptionRef subscription = subscriptionQueryPort
                .findByIdAndCompanyId(command.subscriptionId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));
        subscription.exigirEmpresa(command.companyId());
        SubscriptionItemBillingProfile linea = exigirLineaQueCobra(command);

        BigDecimal quantity = BigDecimal.valueOf(command.overageUnits());
        SubscriptionCharge charge = SubscriptionCharge.create(command.companyId(),
                subscription.id(), command.subscriptionItemId(), ChargeType.OVERAGE,
                command.description(),
                new ServicePeriod(command.servicePeriodStart(), command.servicePeriodEnd()),
                quantity, command.unitAmount(), command.unitAmount().multiply(quantity),
                linea.taxRate(), linea.taxTreatment(), ProrationBasis.of(null, null), null, clock);
        SubscriptionCharge saved = repository.save(charge);

        metrics.chargeAccrued(saved.getChargeType(), saved.getSubtotalAmount());
        audit.chargeAccrued(saved.getId(), saved.getSubscriptionId(), saved.getChargeType(),
                saved.getSubtotalAmount(), saved.getAmendmentId());
        return SubscriptionChargeDto.from(saved);
    }

    /**
     * La línea del contrato es <b>obligatoria</b> aquí, al revés que en el alta
     * general: el excedente sale de {@code subscription_item_limits}, que cuelga de
     * una línea concreta. Y tiene que ser de la misma empresa y estar en un modo
     * que devengue.
     *
     * <p>
     * <b>Devuelve el perfil entero y no solo el modo.</b> La misma lectura que
     * comprueba que la línea cobra trae ya su {@code tax_treatment} y su
     * {@code tax_rate}, así que al cargo no le queda de dónde sacarse un impuesto
     * propio ni a un llamador futuro dónde meter una constante.
     *
     * @return el modo de cobro y el impuesto que el cargo hereda de la línea
     */
    private SubscriptionItemBillingProfile exigirLineaQueCobra(AccrueOverageChargeCommand command) {
        if (command.subscriptionItemId() == null)
            throw new IllegalArgumentException("subscriptionItemId is required for an overage"
                    + " charge: the allowance itself hangs from a contract line");
        SubscriptionItemBillingProfile linea = subscriptionItemValidationPort
                .findBillingProfileInCompany(command.subscriptionItemId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription item not found for company " + command.companyId() + ": "
                                + command.subscriptionItemId()));
        if (!linea.chargeMode().generatesCharge())
            throw new NonBillableSubscriptionItemException(command.subscriptionItemId(),
                    linea.chargeMode());
        return linea;
    }
}
