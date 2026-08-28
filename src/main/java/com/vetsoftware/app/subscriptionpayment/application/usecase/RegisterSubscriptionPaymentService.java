package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.command.RegisterSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.RegisterSubscriptionPaymentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentAuditPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentMetrics;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la plata que entro.
 *
 * <p>
 * <strong>R13 - toda peticion que mueve dinero lleva llave de idempotencia y se
 * busca antes de insertar.</strong> Las constraints unicas
 * ({@code uq_subscription_payments_client_request},
 * {@code uq_subscription_payments_gateway}) convierten el duplicado en un
 * error, pero un 500 en la cara del cliente no es una respuesta idempotente: el
 * operador vuelve a darle al boton y el reintento de la pasarela reaparece cada
 * pocos minutos. La busqueda previa, <strong>dentro de la transaccion</strong>,
 * devuelve el pago que ya se creo con el mismo codigo de estado que la primera
 * vez.
 */
@Observed(name = "subscription.payment.register")
@Service
public class RegisterSubscriptionPaymentService implements RegisterSubscriptionPaymentUseCase {

    private final SubscriptionPaymentRepository repository;
    private final SubscriptionPaymentMetrics metrics;
    private final SubscriptionPaymentAuditPort audit;
    private final Clock clock;

    public RegisterSubscriptionPaymentService(SubscriptionPaymentRepository repository,
            SubscriptionPaymentMetrics metrics, SubscriptionPaymentAuditPort audit, Clock clock) {
        this.repository = repository;
        this.metrics = metrics;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionPaymentDto execute(RegisterSubscriptionPaymentCommand command) {
        Optional<SubscriptionPayment> byClientRequest = findByClientRequestId(command);
        if (byClientRequest.isPresent())
            return SubscriptionPaymentDto.from(byClientRequest.get());

        Optional<SubscriptionPayment> byGateway = findByGatewayReference(command);
        if (byGateway.isPresent())
            return SubscriptionPaymentDto.from(byGateway.get());

        SubscriptionPayment payment = SubscriptionPayment.register(command.companyId(),
                command.amount(), command.currency(), command.paymentMethod(), command.gateway(),
                command.gatewayReference(), command.receivedAt(), command.clientRequestId(),
                LocalDateTime.now(clock));
        SubscriptionPayment saved = repository.save(payment);

        // Los dos caminos de idempotencia de arriba NO pasan por aqui, y es lo
        // correcto:
        // un reintento no es un pago nuevo, y contarlo duplicaria la plata que entro.
        metrics.paymentRegistered(saved.getPaymentMethod(), saved.getStatus());
        audit.paymentRegistered(saved.getId(), saved.getPaymentMethod(), saved.getAmount(),
                saved.getStatus());
        return SubscriptionPaymentDto.from(saved);
    }

    /** Cubre el doble clic del operador que registra un pago manual. */
    private Optional<SubscriptionPayment> findByClientRequestId(
            RegisterSubscriptionPaymentCommand command) {
        if (command.clientRequestId() == null || command.clientRequestId().isBlank())
            return Optional.empty();
        return repository.findByCompanyIdAndClientRequestId(command.companyId(),
                command.clientRequestId());
    }

    /**
     * Cubre el mismo aviso de la pasarela recibido dos veces.
     *
     * <p>
     * <strong>Ojo con el aislamiento aqui:</strong>
     * {@code uq_subscription_payments_gateway} es global, no por empresa, asi que
     * este finder puede devolver el pago de <em>otra</em> clinica. Devolverlo seria
     * filtrar sus importes al tenant que pregunta. Cuando la referencia ya esta
     * tomada por otra empresa se rechaza sin decir de quien es: es un conflicto
     * (409), no un resultado.
     */
    private Optional<SubscriptionPayment> findByGatewayReference(
            RegisterSubscriptionPaymentCommand command) {
        if (command.gateway() == null || command.gatewayReference() == null)
            return Optional.empty();
        Optional<SubscriptionPayment> existing = repository
                .findByGatewayAndGatewayReference(command.gateway(), command.gatewayReference());
        if (existing.isEmpty())
            return Optional.empty();
        if (!existing.get().getCompanyId().equals(command.companyId()))
            throw new IllegalStateException(
                    "gateway reference is already registered by another company");
        return existing;
    }
}
