package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionAmendmentValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.ProrationBasis;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import org.springframework.stereotype.Service;

/**
 * Devenga un cargo: el servicio se prestó, con o sin factura todavía.
 *
 * <p>
 * <b>Resuelve el contrato por la variante acotada por empresa antes de
 * construir nada.</b> No es defensa en profundidad decorativa: sin ella, un
 * {@code subscriptionId} equivocado colgaría el cargo de la clínica A del
 * contrato de la clínica B, y el cargo acabaría facturado en el documento
 * equivocado. La FK compuesta lo impediría en la base, pero con un error de
 * constraint convertido en 500 y a mitad del cierre mensual.
 *
 * <p>
 * <b>Las otras dos referencias del cargo se resuelven igual, y por la misma
 * razon.</b> {@code subscriptionItemId} y {@code amendmentId} tienen tambien su
 * FK compuesta ({@code fk_subscription_charges_item},
 * {@code fk_subscription_charges_amendment}), asi que tampoco se puede colgar
 * el cargo de un padre ajeno; lo que faltaba era la <b>calidad del fallo</b>.
 * Un operador de plataforma que durante el cierre mensual copia el
 * {@code amendmentId} de un otrosi de otra clinica —dos pestanas abiertas, ids
 * consecutivos— recibia un 500 de constraint y tenia que ir al log de la base
 * para saber cual de los cinco ids del cuerpo estaba mal. Ahora recibe un
 * mensaje que lo nombra.
 *
 * <p>
 * Los dos campos son opcionales, asi que la comprobacion solo aplica cuando
 * vienen informados: un cargo puede no nacer de ninguna linea concreta ni de
 * ningun otrosi.
 */
@Observed(name = "subscription.billing.charge.create")
@Service
public class CreateSubscriptionChargeService implements CreateSubscriptionChargeUseCase {

    private final SubscriptionChargeRepository repository;
    private final SubscriptionQueryPort subscriptionQueryPort;
    private final SubscriptionItemValidationPort subscriptionItemValidationPort;
    private final SubscriptionAmendmentValidationPort subscriptionAmendmentValidationPort;
    private final Clock clock;

    public CreateSubscriptionChargeService(SubscriptionChargeRepository repository,
            SubscriptionQueryPort subscriptionQueryPort,
            SubscriptionItemValidationPort subscriptionItemValidationPort,
            SubscriptionAmendmentValidationPort subscriptionAmendmentValidationPort, Clock clock) {
        this.repository = repository;
        this.subscriptionQueryPort = subscriptionQueryPort;
        this.subscriptionItemValidationPort = subscriptionItemValidationPort;
        this.subscriptionAmendmentValidationPort = subscriptionAmendmentValidationPort;
        this.clock = clock;
    }

    @Override
    public SubscriptionChargeDto execute(CreateSubscriptionChargeCommand command) {
        SubscriptionRef subscription = subscriptionQueryPort
                .findByIdAndCompanyId(command.subscriptionId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));
        subscription.exigirEmpresa(command.companyId());
        exigirLineaDeLaEmpresa(command);
        exigirOtrosiDeLaEmpresa(command);
        SubscriptionCharge charge = SubscriptionCharge.create(command.companyId(),
                subscription.id(), command.subscriptionItemId(), command.chargeType(),
                command.description(),
                new ServicePeriod(command.servicePeriodStart(), command.servicePeriodEnd()),
                command.quantity(), command.unitAmount(), command.subtotalAmount(),
                command.taxRate(), command.taxTreatment(),
                ProrationBasis.of(command.prorationDays(), command.periodDays()),
                command.amendmentId(), clock);
        return SubscriptionChargeDto.from(repository.save(charge));
    }

    /**
     * La linea del contrato es opcional; cuando viene, tiene que ser de la misma
     * empresa que el cargo. Es lo que exige {@code fk_subscription_charges_item},
     * comprobado aqui para que el fallo diga cual es el id malo.
     */
    private void exigirLineaDeLaEmpresa(CreateSubscriptionChargeCommand command) {
        if (command.subscriptionItemId() == null)
            return;
        if (!subscriptionItemValidationPort.existsInCompany(command.subscriptionItemId(),
                command.companyId()))
            throw new IllegalArgumentException("Subscription item not found for company "
                    + command.companyId() + ": " + command.subscriptionItemId());
    }

    /**
     * El otrosi es opcional; cuando viene, tiene que ser de la misma empresa que el
     * cargo. Es lo que exige {@code fk_subscription_charges_amendment}.
     */
    private void exigirOtrosiDeLaEmpresa(CreateSubscriptionChargeCommand command) {
        if (command.amendmentId() == null)
            return;
        if (!subscriptionAmendmentValidationPort.existsInCompany(command.amendmentId(),
                command.companyId()))
            throw new IllegalArgumentException("Subscription amendment not found for company "
                    + command.companyId() + ": " + command.amendmentId());
    }
}
