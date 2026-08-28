package com.vetsoftware.app.paymentattempt.application.usecase;

import com.vetsoftware.app.paymentattempt.application.command.RecordPaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.dto.PaymentAttemptDto;
import com.vetsoftware.app.paymentattempt.application.port.in.RecordPaymentAttemptUseCase;
import com.vetsoftware.app.paymentattempt.application.port.out.BillingDocumentValidationPort;
import com.vetsoftware.app.paymentattempt.application.port.out.PaymentAttemptRepository;
import com.vetsoftware.app.paymentattempt.application.port.out.SubscriptionPaymentMethodValidationPort;
import com.vetsoftware.app.paymentattempt.domain.PaymentAttempt;
import com.vetsoftware.app.paymentattempt.domain.RetryBudgetExhaustedException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anota un cobro que reboto y decide si queda presupuesto para otro.
 *
 * <p>
 * <strong>Tres cosas pasan dentro de la misma transaccion, y el orden
 * importa:</strong>
 *
 * <ol>
 * <li>Se resuelven las dos referencias ajenas <em>acotadas por empresa</em>. Un
 * documento o un medio de pago de otra clinica no es un 404 disfrazado: es la
 * fuga que {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}
 * persigue.</li>
 * <li>Se calcula el consecutivo como {@code max + 1} sobre el documento.
 * Dejarselo elegir a quien llama es como se cuelan huecos y colisiones contra
 * {@code uq_payment_attempts_number}; calcularlo fuera de la transaccion es
 * como dos webhooks simultaneos escriben el mismo numero.</li>
 * <li>Se construye el intento <em>antes</em> de mirar el presupuesto, porque
 * quien sabe si gasta o no es el propio agregado
 * ({@link PaymentAttempt#consumesCustomerAttempts()}), no este servicio.</li>
 * </ol>
 *
 * <p>
 * <strong>El fallo propio no gasta presupuesto.</strong> Una credencial mal
 * puesta o una pasarela caida es un {@code CONFIGURATION}: se registra igual
 * -hace falta para poder revisarlo- pero no cuenta contra los cuatro intentos
 * de la ventana ni arranca cobranza contra el cliente.
 *
 * <p>
 * <strong>Y las tres tienen que pasar de una en una.</strong> Ver
 * {@link #execute(RecordPaymentAttemptCommand)}: el consecutivo y el
 * presupuesto son dos lecturas de agregado sobre las que se decide y se
 * escribe, y sin candado dos webhooks simultaneos leen los dos «tres de cuatro
 * gastados» y escriben los dos.
 */
@Observed(name = "payment.attempt.record")
@Service
public class RecordPaymentAttemptService implements RecordPaymentAttemptUseCase {

    private final PaymentAttemptRepository repository;
    private final BillingDocumentValidationPort billingDocumentValidationPort;
    private final SubscriptionPaymentMethodValidationPort paymentMethodValidationPort;
    private final Clock clock;

    public RecordPaymentAttemptService(PaymentAttemptRepository repository,
            BillingDocumentValidationPort billingDocumentValidationPort,
            SubscriptionPaymentMethodValidationPort paymentMethodValidationPort, Clock clock) {
        this.repository = repository;
        this.billingDocumentValidationPort = billingDocumentValidationPort;
        this.paymentMethodValidationPort = paymentMethodValidationPort;
        this.clock = clock;
    }

    /**
     * <strong>El candado sobre el documento y {@code READ_COMMITTED} son las dos
     * mitades del mismo arreglo. Con una sola, el presupuesto queda escrito y no
     * cumplido.</strong>
     *
     * <p>
     * <b>Que se rompia.</b> {@link #nextAttemptNumber} y
     * {@link #rejectIfBudgetExhausted} son lecturas de agregado -un {@code MAX} y
     * un {@code COUNT} sobre el mismo documento- y sobre las dos se decide y se
     * escribe. Sin nada que serialice, dos cobros simultaneos de la misma factura
     * leen los dos «tres de cuatro gastados», los dos pasan la comprobacion y entre
     * los dos <b>cuelan un quinto intento cobrable</b>. No es un descuadre contable
     * que se arregle luego: las redes de tarjeta penalizan el reintento excesivo,
     * asi que el precio se paga en dinero y en reputacion de comercio. La misma
     * carrera duplica el consecutivo, aunque esa mitad al menos es ruidosa —choca
     * contra {@code uq_payment_attempts_number}—; la del presupuesto es silenciosa,
     * y por eso es la peor.
     *
     * <p>
     * <b>Se bloquea el documento de cobro, no el intento.</b> La carrera es sobre
     * <em>insertar</em>, y una fila que aun no existe no se puede bloquear. El
     * documento es el padre comun y el alcance exacto de las dos invariantes -las
     * dos se calculan por {@code (company_id, billing_document_id)}-, asi que el
     * candado no es ni mas ancho ni mas estrecho de lo que hace falta: dos facturas
     * distintas siguen cobrandose en paralelo.
     *
     * <p>
     * <b>Y {@code READ_COMMITTED} no es una preferencia.</b> MySQL corre por
     * defecto en {@code REPEATABLE READ}, e InnoDB fija la foto de lectura de la
     * transaccion en su <b>primera lectura consistente</b>. Aqui esa primera
     * lectura es la validacion del documento, que va <em>antes</em> del candado.
     * Con la foto congelada ahi, el candado haria su trabajo -esperar al rival- y
     * aun asi {@code findMaxAttemptNumber} y {@code countRetryableSince}
     * devolverian los valores <b>anteriores</b> a que el rival confirmara, porque
     * son lecturas no bloqueantes servidas desde esa foto. Es decir: el candado se
     * leeria perfecto en revision y el quinto intento seguiria colandose. Es el
     * mismo defecto que {@code RegisterPaymentRefundService} documenta y mide en
     * {@code PaymentRefundConcurrencyIT}.
     *
     * <p>
     * <b>Dos alternativas consideradas y descartadas.</b> {@code SERIALIZABLE}
     * cierra el agujero pero convierte cada lectura del metodo en una lectura
     * bloqueante y hace del interbloqueo el desenlace normal de dos webhooks sobre
     * el mismo documento. Y apoyarse solo en {@code uq_payment_attempts_number}
     * atrapando el duplicado cierra el consecutivo pero <b>no el presupuesto</b>,
     * que es un {@code COUNT} sobre una ventana y no admite indice unico -la misma
     * situacion de «esto la base no lo puede expresar» que el tope de las
     * devoluciones-, y ademas convierte un reintento legitimo en un 500.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PaymentAttemptDto execute(RecordPaymentAttemptCommand command) {
        if (!billingDocumentValidationPort.existsByIdAndCompanyId(command.billingDocumentId(),
                command.companyId()))
            throw new IllegalArgumentException(
                    "Billing document not found: " + command.billingDocumentId());
        // Admite vacio: un CONFIGURATION puede rebotar antes de llegar a usar
        // medio alguno, y exigirlo obligaria a inventarse uno.
        if (command.paymentMethodId() != null && !paymentMethodValidationPort
                .existsByIdAndCompanyId(command.paymentMethodId(), command.companyId()))
            throw new IllegalArgumentException(
                    "Payment method not found: " + command.paymentMethodId());

        // El candado va ANTES de las dos lecturas de agregado. Es lo unico que
        // serializa a dos intentos concurrentes sobre la misma factura, y por eso
        // no puede bajar ni una linea: entre el y el save no cabe ninguna lectura
        // de la que dependa una decision.
        billingDocumentValidationPort.lockByIdAndCompanyId(command.billingDocumentId(),
                command.companyId());

        LocalDateTime now = LocalDateTime.now(clock);
        int attemptNumber = nextAttemptNumber(command);
        PaymentAttempt attempt = PaymentAttempt.attempted(command.companyId(),
                command.billingDocumentId(), command.paymentMethodId(), attemptNumber,
                command.gateway(), command.requestedAmount(), command.gatewayDeclineCode(),
                command.declineKind(), command.attemptedAt(), command.nextAttemptAt(), now);

        rejectIfBudgetExhausted(command, attempt, now);
        return PaymentAttemptDto.from(repository.save(attempt));
    }

    private int nextAttemptNumber(RecordPaymentAttemptCommand command) {
        return repository.findMaxAttemptNumber(command.companyId(), command.billingDocumentId())
                .orElse(0) + 1;
    }

    /**
     * Cuatro intentos imputables en dos semanas. Solo se comprueba cuando el
     * intento entrante gasta presupuesto: registrar el quinto fallo propio seguido
     * tiene que seguir siendo posible, porque es la evidencia de que la averia es
     * nuestra.
     */
    private void rejectIfBudgetExhausted(RecordPaymentAttemptCommand command,
            PaymentAttempt attempt, LocalDateTime now) {
        if (!attempt.consumesCustomerAttempts())
            return;
        int spent = repository.countRetryableSince(command.companyId(), command.billingDocumentId(),
                now.minus(PaymentAttempt.RETRY_WINDOW));
        if (spent >= PaymentAttempt.MAX_SOFT_ATTEMPTS)
            throw new RetryBudgetExhaustedException(command.billingDocumentId(),
                    PaymentAttempt.MAX_SOFT_ATTEMPTS);
    }
}
