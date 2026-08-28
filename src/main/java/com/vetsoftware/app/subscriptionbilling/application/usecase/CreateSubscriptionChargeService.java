package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionAmendmentValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.NonBillableSubscriptionItemException;
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
 * <b>Y la linea, ademas de existir, tiene que cobrar</b> (R-TRIAL-14). La FK no
 * dice nada de eso: una linea en {@code TRIAL} la cumple igual de bien que una
 * {@code PAID}, y como la linea gratuita <b>conserva su tarifa real</b> el
 * cargo resultante no sale en cero sino por el importe completo. Esta es la
 * puerta de entrada del devengo, asi que cerrarla aqui es lo que impide que esa
 * fila llegue a existir; la consulta que selecciona lo que se factura vuelve a
 * filtrar por su cuenta, para las filas devengadas antes de que esta guarda
 * existiera. Lo que <b>no</b> se mira en ningun punto es el estado del contrato
 * (R-TRIAL-13): un contrato {@code TRIALING} devenga sus lineas {@code PAID}
 * con total normalidad.
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
    private final SubscriptionBillingMetrics metrics;
    private final SubscriptionBillingAuditPort audit;
    private final Clock clock;

    public CreateSubscriptionChargeService(SubscriptionChargeRepository repository,
            SubscriptionQueryPort subscriptionQueryPort,
            SubscriptionItemValidationPort subscriptionItemValidationPort,
            SubscriptionAmendmentValidationPort subscriptionAmendmentValidationPort,
            SubscriptionBillingMetrics metrics, SubscriptionBillingAuditPort audit, Clock clock) {
        this.repository = repository;
        this.subscriptionQueryPort = subscriptionQueryPort;
        this.subscriptionItemValidationPort = subscriptionItemValidationPort;
        this.subscriptionAmendmentValidationPort = subscriptionAmendmentValidationPort;
        this.metrics = metrics;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public SubscriptionChargeDto execute(CreateSubscriptionChargeCommand command) {
        SubscriptionRef subscription = subscriptionQueryPort
                .findByIdAndCompanyId(command.subscriptionId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription not found: " + command.subscriptionId()));
        subscription.exigirEmpresa(command.companyId());
        exigirLineaQueCobra(command);
        exigirOtrosiDeLaEmpresa(command);
        SubscriptionCharge charge = SubscriptionCharge.create(command.companyId(),
                subscription.id(), command.subscriptionItemId(), command.chargeType(),
                command.description(),
                new ServicePeriod(command.servicePeriodStart(), command.servicePeriodEnd()),
                command.quantity(), command.unitAmount(), command.subtotalAmount(),
                command.taxRate(), command.taxTreatment(),
                ProrationBasis.of(command.prorationDays(), command.periodDays()),
                command.amendmentId(), clock);
        SubscriptionCharge saved = repository.save(charge);

        // El hecho contable que no tenia contador: cuando esto lo emite el cierre de
        // mes, «cuantos cargos salieron anoche y por cuanto» solo se podia responder
        // abriendo la base de produccion. subscription_charges es ademas la unica tabla
        // del bloque sin llave antiduplicados, asi que el conteo es la unica forma de
        // ver que el barrido se reinicio a mitad de lote y volvio a devengar.
        metrics.chargeAccrued(saved.getChargeType(), saved.getSubtotalAmount());
        audit.chargeAccrued(saved.getId(), saved.getSubscriptionId(), saved.getChargeType(),
                saved.getSubtotalAmount(), saved.getAmendmentId());
        return SubscriptionChargeDto.from(saved);
    }

    /**
     * La linea del contrato es opcional; cuando viene, tiene que ser de la misma
     * empresa que el cargo —lo que exige {@code fk_subscription_charges_item},
     * comprobado aqui para que el fallo diga cual es el id malo— <b>y tiene que
     * estar en un modo que devengue</b>.
     *
     * <p>
     * Las dos negativas son distintas a proposito. Que no exista o sea de otra
     * empresa es un id mal escrito: {@code IllegalArgumentException} y 400. Que
     * exista y no cobre es un conflicto de estado —el id esta bien, la linea esta
     * en prueba—: {@link NonBillableSubscriptionItemException} y 409, con el modo
     * dentro del mensaje para que el operador sepa si toca esperar a la conversion
     * o revisar el tope del plan.
     */
    private void exigirLineaQueCobra(CreateSubscriptionChargeCommand command) {
        if (command.subscriptionItemId() == null)
            return;
        ItemChargeMode chargeMode = subscriptionItemValidationPort
                .findChargeModeInCompany(command.subscriptionItemId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription item not found for company " + command.companyId() + ": "
                                + command.subscriptionItemId()));
        if (!chargeMode.generatesCharge())
            throw new NonBillableSubscriptionItemException(command.subscriptionItemId(),
                    chargeMode);
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
