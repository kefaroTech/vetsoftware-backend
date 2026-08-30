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
 * {@link ContractPaymentPort}, que el dia que exista pasarela hara I/O HTTP.
 * Una llamada remota dentro de una transaccion retiene la conexion del pool y
 * los locks del contrato mientras dura, y la regla dura
 * {@code SIN_IO_EXTERNO_EN_TRANSACCION} <em>sigue la cadena de llamadas</em>
 * hasta aqui: anotar este metodo romperia el build en cuanto el adaptador
 * simulado se sustituya por el real. La escritura que si necesita transaccion
 * —el cambio de estado— la hace {@link ChangeSubscriptionStatusUseCase}, que
 * abre la suya.
 *
 * <p>
 * <strong>Lo que se lee se lee ANTES de cobrar.</strong> El contrato se carga
 * al principio para tener numero y ciclo sin dejar una consulta colgando
 * despues de la llamada remota.
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

        // Un contrato que ya esta ACTIVE no tiene primer periodo que cobrar. Se
        // comprueba aqui y no se delega en la excepcion de transicion invalida porque
        // un reintento legitimo no es un error: este metodo lo dispara un
        // afterCommit, y esos se repiten.
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        ContractPaymentOutcome outcome = paymentPort.chargeFirstPeriod(command.companyId(),
                subscription.getId(), subscription.getSubscriptionNumber(),
                subscription.getBillingCycle());

        if (!outcome.approved()) {
            // Ni se degrada ni se lanza: el contrato se queda donde nacio y el cliente
            // conserva su acceso. Degradar por un cobro fallido es trabajo de `dunning`,
            // que mira la deuda real y no un intento suelto. Ver el javadoc del puerto.
            log.warn("El primer cobro del contrato {} no se aprobo; queda en {}. motivo={}",
                    subscription.getSubscriptionNumber(), subscription.getStatus(),
                    outcome.declineReason());
            return;
        }

        changeStatusUseCase.execute(new ChangeSubscriptionStatusCommand(subscription.getId(),
                command.companyId(), SubscriptionStatus.ACTIVE,
                SubscriptionStatusChangeReason.PAYMENT_RECEIVED, ACTOR));
    }
}
