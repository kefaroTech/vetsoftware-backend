package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import com.vetsoftware.app.subscriptionpayment.domain.OverAppliedSourceException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotConfirmedException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica un origen -un pago o un saldo a favor- contra una factura.
 *
 * <p>
 * Aqui viven las tres reglas que la base no puede imponer:
 *
 * <ul>
 * <li><strong>R3</strong>: la suma de lo aplicado desde un origen nunca supera
 * ese origen. Se comprueba con el origen tomado en <em>bloqueo pesimista</em>
 * dentro de la misma transaccion; el bloqueo es lo que serializa el
 * <em>read-then-write</em>. Sin el, dos aplicaciones concurrentes leen la misma
 * suma y las dos pasan, y la cartera acaba cuadrando con plata que no entro.
 * <li><strong>R4</strong>: {@code settled_amount} del destino se
 * <em>recalcula</em> desde las aplicaciones en esta misma transaccion, nunca se
 * acumula.
 * <li><strong>R13</strong>: la peticion lleva llave de idempotencia y <em>se
 * busca antes de insertar</em>.
 * </ul>
 *
 * <p>
 * <strong>R13 no es redundante con R3, y confundirlas cuesta dinero.</strong>
 * R3 acota <em>cuanto</em> se ha aplicado desde un origen; no sabe nada de
 * <em>cuantas veces</em> se pidio. Con un pago de 100 y dos peticiones
 * identicas de 50 -un doble clic, o un reintento del navegador sobre una
 * respuesta que se perdio- el total aplicado es 100, R3 dice que si, y la
 * factura queda saldada por el doble de lo que el operador quiso. Y los numeros
 * cuadran, que es lo que hace este error tan dificil de discutir mirando la
 * cartera. La llave es lo unico que distingue «dos aplicaciones de 50» de «la
 * misma aplicacion de 50 pedida dos veces».
 *
 * <p>
 * <strong>Orden de bloqueos.</strong> Los documentos se toman por id ascendente
 * y el origen despues. Con dos transacciones aplicando cruzado -una la nota
 * credito 7 sobre la factura 9 y la otra al reves- un orden por rol en vez de
 * por id se abraza y el motor mata una de las dos.
 */
@Observed(name = "subscription.payment.apply")
@Service
public class ApplyBillingDocumentService implements ApplyBillingDocumentUseCase {

    private final BillingDocumentApplicationRepository applicationRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final BillingDocumentQueryPort billingDocumentQueryPort;
    private final BillingDocumentSettlementPort settlementPort;
    private final DunningReevaluationPort dunningReevaluationPort;
    private final Clock clock;

    public ApplyBillingDocumentService(BillingDocumentApplicationRepository applicationRepository,
            SubscriptionPaymentRepository paymentRepository,
            BillingDocumentQueryPort billingDocumentQueryPort,
            BillingDocumentSettlementPort settlementPort,
            DunningReevaluationPort dunningReevaluationPort, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.paymentRepository = paymentRepository;
        this.billingDocumentQueryPort = billingDocumentQueryPort;
        this.settlementPort = settlementPort;
        this.dunningReevaluationPort = dunningReevaluationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillingDocumentApplicationDto execute(ApplyBillingDocumentCommand command) {
        if (command.sourceKind() == null)
            throw new IllegalArgumentException("sourceKind is required");
        if (command.targetDocumentId() == null)
            throw new IllegalArgumentException("targetDocumentId is required");

        lockDocumentsInAscendingIdOrder(command);
        BillingDocumentRef target = resolveDocument(command.targetDocumentId(),
                command.companyId());
        ResolvedSource source = resolveAndLockSource(command);

        // R13, y el orden importa: la busqueda va DESPUES de tomar el candado del
        // origen. Un reintento que llegue segundo se queda esperando ahi y, cuando
        // entra, lee la fila que el primero acaba de confirmar. Al reves -buscar y
        // luego bloquear- los dos leerian "no existe" y los dos insertarian.
        Optional<BillingDocumentApplication> alreadyApplied = findByClientRequestId(command);
        if (alreadyApplied.isPresent()) {
            // Ni se inserta ni se recalcula: devolver la aplicacion que nacio la primera
            // vez es lo que hace que el reintento no mueva el saldo de la factura.
            return BillingDocumentApplicationDto.from(alreadyApplied.get());
        }

        requireWithinSource(source, command.appliedAmount());
        LocalDateTime appliedAt = LocalDateTime.now(clock);
        BillingDocumentApplication application = build(command, target, source, appliedAt);

        BillingDocumentApplicationDto dto = BillingDocumentApplicationDto
                .from(applicationRepository.save(application));
        settlementPort.recalculateSettledAmount(target.id(), command.companyId());
        dunningReevaluationPort.reevaluate(target.id(), command.companyId());
        return dto;
    }

    /**
     * El origen ya resuelto y bloqueado, con lo que R3 necesita para decidir:
     * cuanto vale y cuanto se ha aplicado ya desde el.
     */
    private record ResolvedSource(ApplicationSourceKind kind, Long sourceId,
            BigDecimal sourceAmount, BigDecimal alreadyApplied, Long paymentId,
            BillingDocumentRef creditNote) {
    }

    /**
     * Toma el bloqueo de los documentos implicados en orden de id ascendente, que
     * es lo unico que garantiza que dos transacciones concurrentes no se abracen.
     */
    private void lockDocumentsInAscendingIdOrder(ApplyBillingDocumentCommand command) {
        Long first = command.targetDocumentId();
        Long second = command.sourceDocumentId();
        if (second == null || second.equals(first)) {
            billingDocumentQueryPort.lockByIdAndCompanyId(first, command.companyId());
            return;
        }
        Long lower = first.compareTo(second) <= 0 ? first : second;
        Long higher = first.compareTo(second) <= 0 ? second : first;
        billingDocumentQueryPort.lockByIdAndCompanyId(lower, command.companyId());
        billingDocumentQueryPort.lockByIdAndCompanyId(higher, command.companyId());
    }

    /**
     * Resuelve el origen y lo deja bloqueado. Es el ultimo candado que se toma, y
     * el que serializa tanto R3 como la busqueda de idempotencia.
     */
    private ResolvedSource resolveAndLockSource(ApplyBillingDocumentCommand command) {
        return switch (command.sourceKind()) {
            case PAYMENT -> {
                if (command.paymentId() == null)
                    throw new IllegalArgumentException(
                            "paymentId is required for a PAYMENT source");
                SubscriptionPayment payment = paymentRepository
                        .lockByIdAndCompanyId(command.paymentId(), command.companyId())
                        .orElseThrow(() -> new SubscriptionPaymentNotFoundException(
                                command.paymentId()));
                if (!payment.countsAsSettlement())
                    throw new SubscriptionPaymentNotConfirmedException(payment.getId());
                yield new ResolvedSource(ApplicationSourceKind.PAYMENT, payment.getId(),
                        payment.getAmount(), applicationRepository.sumAppliedFromPayment(
                                payment.getId(), command.companyId()),
                        payment.getId(), null);
            }
            case CREDIT_NOTE -> {
                if (command.sourceDocumentId() == null)
                    throw new IllegalArgumentException(
                            "sourceDocumentId is required for a CREDIT_NOTE source");
                // Ya quedo bloqueada arriba, junto con la factura destino y en orden de id.
                BillingDocumentRef creditNote = resolveDocument(command.sourceDocumentId(),
                        command.companyId());
                yield new ResolvedSource(ApplicationSourceKind.CREDIT_NOTE, creditNote.id(),
                        creditNote.totalAmount(), applicationRepository
                                .sumAppliedFromSourceDocument(creditNote.id(), command.companyId()),
                        null, creditNote);
            }
        };
    }

    /** Sin llave no hay nada que deduplicar: es el comportamiento de siempre. */
    private Optional<BillingDocumentApplication> findByClientRequestId(
            ApplyBillingDocumentCommand command) {
        if (command.clientRequestId() == null || command.clientRequestId().isBlank())
            return Optional.empty();
        return applicationRepository.findByCompanyIdAndClientRequestId(command.companyId(),
                command.clientRequestId());
    }

    private BillingDocumentApplication build(ApplyBillingDocumentCommand command,
            BillingDocumentRef target, ResolvedSource source, LocalDateTime appliedAt) {
        return switch (source.kind()) {
            case PAYMENT -> BillingDocumentApplication.fromPayment(command.companyId(), target,
                    source.paymentId(), command.appliedAmount(), command.clientRequestId(),
                    appliedAt);
            // Este es el camino que salda una factura sin que entre un peso, y el que la
            // primera version del modelo no podia representar.
            case CREDIT_NOTE -> BillingDocumentApplication.fromCreditNote(command.companyId(),
                    target, source.creditNote(), command.appliedAmount(), command.clientRequestId(),
                    appliedAt);
        };
    }

    /**
     * Resolucion <strong>acotada por empresa</strong>: la factura de otra clinica
     * no se resuelve, asi que no hay forma de colgarle una aplicacion
     * ({@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA}).
     */
    private BillingDocumentRef resolveDocument(Long documentId, Long companyId) {
        return billingDocumentQueryPort.findByIdAndCompanyId(documentId, companyId).orElseThrow(
                () -> new IllegalArgumentException("BillingDocument not found: " + documentId));
    }

    /**
     * R3. La suma ya aplicada es <strong>neta</strong>: las contra-aplicaciones son
     * negativas, asi que revertir una aplicacion libera su importe para volver a
     * aplicarlo.
     */
    private static void requireWithinSource(ResolvedSource source, BigDecimal requested) {
        if (requested == null)
            throw new IllegalArgumentException("appliedAmount is required");
        BigDecimal available = source.sourceAmount().subtract(source.alreadyApplied());
        if (requested.compareTo(available) > 0)
            throw new OverAppliedSourceException(source.kind(), source.sourceId(), available,
                    requested);
    }
}
