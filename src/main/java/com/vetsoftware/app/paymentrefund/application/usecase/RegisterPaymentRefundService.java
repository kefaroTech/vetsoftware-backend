package com.vetsoftware.app.paymentrefund.application.usecase;

import com.vetsoftware.app.paymentrefund.application.command.RegisterPaymentRefundCommand;
import com.vetsoftware.app.paymentrefund.application.dto.PaymentRefundDto;
import com.vetsoftware.app.paymentrefund.application.port.in.RegisterPaymentRefundUseCase;
import com.vetsoftware.app.paymentrefund.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.paymentrefund.application.port.out.SubscriptionPaymentQueryPort;
import com.vetsoftware.app.paymentrefund.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import com.vetsoftware.app.paymentrefund.domain.SubscriptionPaymentRef;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra la plata que se devuelve.
 *
 * <p>
 * <strong>El orden de los pasos es la mitad de la correccion, no una
 * preferencia de estilo.</strong> Primero la idempotencia, para que un
 * reintento no llegue siquiera a mirar el pago; despues el candado sobre la
 * fila del pago; y solo entonces la suma de lo ya devuelto. Invertir los dos
 * ultimos deja el tope escrito y no cumplido: dos devoluciones parciales
 * simultaneas leen la misma suma, las dos pasan la comprobacion y entre las dos
 * sacan mas dinero del que entro. No da error, no deja log, y se descubre
 * cuadrando la caja.
 *
 * <p>
 * <strong>R13 - toda peticion que mueve dinero lleva llave de idempotencia y se
 * busca antes de insertar.</strong> {@code uq_payment_refunds_client_request}
 * convierte el duplicado en un error, pero un 500 en la cara del operador no es
 * una respuesta idempotente: vuelve a darle al boton.
 */
@Observed(name = "payment.refund.register")
@Service
public class RegisterPaymentRefundService implements RegisterPaymentRefundUseCase {

    private final PaymentRefundRepository repository;
    private final SubscriptionPaymentQueryPort subscriptionPaymentQueryPort;
    private final BillingDocumentValidationPort billingDocumentValidationPort;
    private final SystemUserValidationPort systemUserValidationPort;
    private final Clock clock;

    public RegisterPaymentRefundService(PaymentRefundRepository repository,
            SubscriptionPaymentQueryPort subscriptionPaymentQueryPort,
            BillingDocumentValidationPort billingDocumentValidationPort,
            SystemUserValidationPort systemUserValidationPort, Clock clock) {
        this.repository = repository;
        this.subscriptionPaymentQueryPort = subscriptionPaymentQueryPort;
        this.billingDocumentValidationPort = billingDocumentValidationPort;
        this.systemUserValidationPort = systemUserValidationPort;
        this.clock = clock;
    }

    /**
     * <strong>{@code READ_COMMITTED} no es una preferencia: sin el, el candado no
     * sirve para nada y el tope queda escrito y no cumplido.</strong>
     *
     * <p>
     * MySQL corre por defecto en {@code REPEATABLE READ}, e InnoDB fija la foto de
     * lectura de la transaccion en su <b>primera lectura consistente</b>. En este
     * metodo esa primera lectura es la busqueda por llave de idempotencia, que va
     * <em>antes</em> del candado. Con la foto congelada ahi,
     * {@code sumRefundedByPaymentAndCompanyId} devuelve lo devuelto <b>antes</b> de
     * que la devolucion rival se confirmara —el candado no refresca nada, y menos
     * aun sobre {@code payment_refunds}, que es otra tabla—: las dos devoluciones
     * pasan el tope y entre las dos sacan mas dinero del que entro.
     *
     * <p>
     * Medido, no supuesto: {@code PaymentRefundConcurrencyIT} reproduce el
     * entrelazado en dos conexiones y, bajo {@code REPEATABLE READ}, la suma vale
     * {@code 0.00} cuando ya hay 500.000 confirmados. Bajo {@code READ_COMMITTED}
     * cada lectura toma foto nueva y vale {@code 500000.00}, que es lo correcto.
     *
     * <p>
     * <b>La trampa es que el defecto depende de la llave de idempotencia.</b> Sin
     * {@code clientRequestId} no hay lectura previa, la foto se toma despues del
     * candado y todo funciona; con ella, falla. Es decir: el camino que R13 obliga
     * a usar para mover dinero era justo el vulnerable.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentRefundDto execute(RegisterPaymentRefundCommand command) {
        Optional<PaymentRefund> existing = findByClientRequestId(command);
        if (existing.isPresent())
            return PaymentRefundDto.from(existing.get());

        // El candado va ANTES de la suma. Es lo unico que serializa a dos
        // devoluciones concurrentes sobre el mismo pago.
        subscriptionPaymentQueryPort.lockByIdAndCompanyId(command.paymentId(), command.companyId());

        SubscriptionPaymentRef payment = subscriptionPaymentQueryPort
                .findByIdAndCompanyId(command.paymentId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + command.paymentId()));

        validateSourceDocument(command);
        validateAuthorizer(command);

        BigDecimal alreadyRefunded = repository
                .sumRefundedByPaymentAndCompanyId(command.paymentId(), command.companyId());

        PaymentRefund refund = PaymentRefund.register(payment, alreadyRefunded,
                command.sourceDocumentId(), command.amount(), command.method(),
                command.destinationReference(), command.refundedAt(), command.valueDate(),
                command.reasonCode(), command.reason(), command.authorizedBySystemUserId(),
                command.clientRequestId(), LocalDateTime.now(clock));
        return PaymentRefundDto.from(repository.save(refund));
    }

    /** Cubre el doble clic del operador que registra la devolucion. */
    private Optional<PaymentRefund> findByClientRequestId(RegisterPaymentRefundCommand command) {
        if (command.clientRequestId() == null || command.clientRequestId().isBlank())
            return Optional.empty();
        return repository.findByCompanyIdAndClientRequestId(command.companyId(),
                command.clientRequestId());
    }

    /**
     * El documento de origen es opcional -una devolucion puede no venir de ninguna
     * factura- pero si viene tiene que ser de la misma empresa, o la FK compuesta
     * lo rechazaria mas tarde y como un error de integridad.
     */
    private void validateSourceDocument(RegisterPaymentRefundCommand command) {
        if (command.sourceDocumentId() == null)
            return;
        if (!billingDocumentValidationPort.existsByIdAndCompanyId(command.sourceDocumentId(),
                command.companyId()))
            throw new IllegalArgumentException(
                    "Billing document not found: " + command.sourceDocumentId());
    }

    /** Sin firma que exista de verdad, la autorizacion no prueba nada. */
    private void validateAuthorizer(RegisterPaymentRefundCommand command) {
        if (!systemUserValidationPort.existsById(command.authorizedBySystemUserId()))
            throw new IllegalArgumentException(
                    "System user not found: " + command.authorizedBySystemUserId());
    }
}
