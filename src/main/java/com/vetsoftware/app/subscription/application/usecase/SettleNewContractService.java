package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.command.SettleNewContractCommand;
import com.vetsoftware.app.subscription.application.dto.ContractPaymentOutcome;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.in.SettleNewContractUseCase;
import com.vetsoftware.app.subscription.application.port.out.ContractPaymentPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Cobra el primer periodo y activa el contrato si el cobro se aprueba.
 *
 * <p>
 * <strong>&#9940; Esta clase NO lleva {@code @Transactional}, y quitarselo no
 * es un descuido: es el requisito.</strong> Llama a
 * {@link ContractPaymentPort}, que hace I/O HTTP contra Wompi. Una llamada
 * remota dentro de una transaccion retiene la conexion del pool y los locks del
 * contrato mientras dura, y la regla dura {@code SIN_IO_EXTERNO_EN_TRANSACCION}
 * <em>sigue la cadena de llamadas</em> hasta aqui. La escritura que si necesita
 * transaccion —el cambio de estado— la hace
 * {@link ChangeSubscriptionStatusUseCase}, que abre la suya.
 *
 * <p>
 * <strong>La prueba no se cobra.</strong> Un contrato {@code TRIALING} no tiene
 * primer periodo que cobrar todavia: el mandato ya quedo guardado y el cobro
 * real llega cuando el ciclo recurrente factura el primer periodo de pago.
 * Fuera de {@code TRIALING} se cobra <strong>siempre</strong>, incluido un
 * contrato ya {@code ACTIVE}: la idempotencia no la da el estado del contrato
 * sino la referencia {@code VS-<numero>-P1} que gestiona
 * {@code paymentgateway}, asi que un reintento legitimo de este metodo
 * -{@code afterCommit} puede repetirse- no cobra dos veces.
 */
@Observed(name = "subscription.settle.new")
@Service
public class SettleNewContractService implements SettleNewContractUseCase {

    private static final Logger log = LoggerFactory.getLogger(SettleNewContractService.class);

    /** Quien firma la activacion en la bitacora. */
    private static final String ACTOR = "quote-acceptance";

    private final SubscriptionRepository repository;
    private final ContractPaymentPort paymentPort;
    private final ChangeSubscriptionStatusUseCase changeStatusUseCase;

    public SettleNewContractService(SubscriptionRepository repository,
            ContractPaymentPort paymentPort, ChangeSubscriptionStatusUseCase changeStatusUseCase) {
        this.repository = repository;
        this.paymentPort = paymentPort;
        this.changeStatusUseCase = changeStatusUseCase;
    }

    @Override
    public void execute(SettleNewContractCommand command) {
        Subscription subscription = repository
                .findByIdAndCompanyId(command.subscriptionId(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));

        if (subscription.getStatus() == SubscriptionStatus.TRIALING) {
            return;
        }

        ContractPaymentOutcome outcome = paymentPort.chargeFirstPeriod(command.companyId(),
                subscription.getId(), subscription.getSubscriptionNumber(),
                subscription.getBillingCycle(), subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd());

        if (!outcome.approved()) {
            // Ni se degrada ni se lanza: el contrato se queda donde nacio y el cliente
            // conserva su acceso. Degradar por un cobro fallido es trabajo de `dunning`,
            // que mira la deuda real y no un intento suelto. Ver el javadoc del puerto.
            log.warn("El primer cobro del contrato {} no se aprobo; queda en {}. motivo={}",
                    subscription.getSubscriptionNumber(), subscription.getStatus(),
                    outcome.declineReason());
            return;
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        activate(command, subscription, outcome);
    }

    /**
     * El dinero ya salio de la tarjeta del cliente. Si la activacion falla aqui, el
     * contrato queda cobrado y sin servicio -un caso muy distinto de un cobro
     * rechazado, que no puede acabar en el mismo {@code WARN} que el llamador usa
     * para «no se pudo liquidar»: hace falta el {@link Throwable} completo y los
     * datos para encontrar la fila a mano.
     */
    private void activate(SettleNewContractCommand command, Subscription subscription,
            ContractPaymentOutcome outcome) {
        try {
            changeStatusUseCase.execute(new ChangeSubscriptionStatusCommand(subscription.getId(),
                    command.companyId(), SubscriptionStatus.ACTIVE,
                    SubscriptionStatusChangeReason.PAYMENT_RECEIVED, ACTOR));
        } catch (RuntimeException exception) {
            log.error(
                    "Cobro aprobado sin activar el contrato: empresa={} contrato={} referencia={}",
                    command.companyId(), subscription.getSubscriptionNumber(), outcome.reference(),
                    exception);
        }
    }
}
