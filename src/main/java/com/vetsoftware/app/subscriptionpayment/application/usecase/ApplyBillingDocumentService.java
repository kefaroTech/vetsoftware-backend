package com.vetsoftware.app.subscriptionpayment.application.usecase;

import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.port.in.ApplyBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentApplicationRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.BillingDocumentSettlementPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.CustomerCreditQueryPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.DunningReevaluationPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentAuditPort;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentMetrics;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.application.port.out.WithholdingQueryPort;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentRef;
import com.vetsoftware.app.subscriptionpayment.domain.CustomerCreditLotRef;
import com.vetsoftware.app.subscriptionpayment.domain.OverAppliedSourceException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotConfirmedException;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import com.vetsoftware.app.subscriptionpayment.domain.WithholdingRef;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica un origen contra una factura. <strong>Los seis</strong>.
 *
 * <p>
 * <strong>Cuatro de ellos se rechazaban por nombre y eso tenia consecuencias
 * caras.</strong> El esquema admitia seis desde el changeset 253 y este
 * servicio solo sabia escribir el pago y la nota credito; los otros cuatro
 * morian en un {@code IllegalArgumentException} que decia, literalmente, que el
 * origen existia pero no tenia camino de escritura. El peor caso es
 * {@code WITHHOLDING}: un cliente gira 205.850 de una factura de 213.010
 * practicando 7.160 de retencion, el saldo queda sin cerrar, pasan los cinco
 * dias de gracia y la cuenta cae a <em>solo lectura por una deuda que
 * fiscalmente no existe</em> -esa plata esta en la DIAN a nombre de VetSoftware
 * y el cliente tiene el certificado-. El sistema tenia razon segun sus propios
 * numeros, que es lo que hacia el fallo tan dificil de ver.
 *
 * <p>
 * <strong>Que sirve de techo de R3 en cada origen.</strong> En los cuatro que
 * apuntan a una fila, esa fila: el importe del pago, el total de la nota
 * credito, el importe de la retencion, lo concedido por el lote de saldo a
 * favor. En los dos que <em>no</em> apuntan a nada -{@code ROUNDING} y
 * {@code WRITE_OFF}- el techo es <strong>el saldo pendiente de la propia
 * factura</strong>: no hay origen del que pasarse, pero si hay algo que no se
 * puede exceder, porque castigar mas deuda de la que se debe deja la factura
 * con saldo negativo.
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
 * <strong>El bloqueo por si solo no basta, y esa es la leccion cara.</strong>
 * R3 se comprueba sobre una suma que se lee <em>despues</em> del candado, y en
 * {@code REPEATABLE READ} esa lectura sale de la foto que InnoDB congelo en la
 * primera lectura consistente del metodo —la resolucion de la factura destino,
 * que va antes—. El candado serializa, pero lo que se lee dentro del candado es
 * viejo. Por eso {@code execute} corre en {@code READ_COMMITTED}: ver su
 * javadoc.
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
    private final WithholdingQueryPort withholdingQueryPort;
    private final CustomerCreditQueryPort customerCreditQueryPort;
    private final BillingDocumentSettlementPort settlementPort;
    private final DunningReevaluationPort dunningReevaluationPort;
    private final SubscriptionPaymentMetrics metrics;
    private final SubscriptionPaymentAuditPort audit;
    private final Clock clock;

    public ApplyBillingDocumentService(BillingDocumentApplicationRepository applicationRepository,
            SubscriptionPaymentRepository paymentRepository,
            BillingDocumentQueryPort billingDocumentQueryPort,
            WithholdingQueryPort withholdingQueryPort,
            CustomerCreditQueryPort customerCreditQueryPort,
            BillingDocumentSettlementPort settlementPort,
            DunningReevaluationPort dunningReevaluationPort, SubscriptionPaymentMetrics metrics,
            SubscriptionPaymentAuditPort audit, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.paymentRepository = paymentRepository;
        this.billingDocumentQueryPort = billingDocumentQueryPort;
        this.withholdingQueryPort = withholdingQueryPort;
        this.customerCreditQueryPort = customerCreditQueryPort;
        this.settlementPort = settlementPort;
        this.dunningReevaluationPort = dunningReevaluationPort;
        this.metrics = metrics;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * <strong>{@code READ_COMMITTED} no es una preferencia: sin el, los candados de
     * este metodo estan puestos y no protegen.</strong>
     *
     * <p>
     * MySQL corre por defecto en {@code REPEATABLE READ}, e InnoDB fija la foto de
     * lectura de la transaccion en su <b>primera lectura consistente</b>. Aqui esa
     * primera lectura es {@link #resolveDocument} sobre la factura destino, que va
     * <em>antes</em> de {@link #resolveAndLockSource}. Con la foto congelada ahi,
     * todo lo que viene despues —las cuatro {@code sumAppliedFrom...} de R3 y la
     * busqueda por llave de idempotencia de R13— lee el mundo tal como estaba
     * <b>antes</b> de que la transaccion rival confirmara. Tomar el candado no
     * refresca esa foto, y menos aun sobre {@code billing_document_applications},
     * que es otra tabla.
     *
     * <p>
     * Es exactamente el defecto que ya cobro en
     * {@code RegisterPaymentRefundService}: el candado escrito, el tope escrito, y
     * las dos devoluciones pasando igual. Bajar el aislamiento hace que cada
     * lectura tome foto nueva, que es lo que convierte el candado en serializacion
     * de verdad.
     *
     * <p>
     * <b>Alcanza a los cuatro origenes que apuntan a una fila, no solo al saldo a
     * favor.</b> El candado del pago se toma en {@code resolveAndLockSource},
     * <em>despues</em> de esa primera lectura, asi que un pago aplicado a dos
     * facturas a la vez tenia el mismo agujero. Solo {@code CREDIT_NOTE} se
     * salvaba, porque su candado se toma en
     * {@link #lockDocumentsInAscendingIdOrder}, antes de cualquier lectura
     * consistente.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        BillingDocumentApplication persisted = applicationRepository.save(application);
        BillingDocumentApplicationDto dto = BillingDocumentApplicationDto.from(persisted);

        // source.kind separa el pago que entro de la nota credito y de la retencion:
        // saldan igual pero no traen un peso, y confundirlos es como se cree haber
        // cobrado un mes que en realidad se descontó.
        metrics.applicationRecorded(persisted.getSourceKind());
        audit.documentApplied(persisted.getId(), persisted.getTargetDocument().id(),
                persisted.getSourceKind(), persisted.getAppliedAmount());
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
            BillingDocumentRef creditNote, Long withholdingId, Long creditEntryId) {

        /** Los cuatro origenes que apuntan a una fila. */
        static ResolvedSource pointingTo(ApplicationSourceKind kind, Long sourceId,
                BigDecimal sourceAmount, BigDecimal alreadyApplied, Long paymentId,
                BillingDocumentRef creditNote, Long withholdingId, Long creditEntryId) {
            return new ResolvedSource(kind, sourceId, sourceAmount, alreadyApplied, paymentId,
                    creditNote, withholdingId, creditEntryId);
        }
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
                yield ResolvedSource.pointingTo(ApplicationSourceKind.PAYMENT, payment.getId(),
                        payment.getAmount(), applicationRepository
                                .sumAppliedFromPayment(payment.getId(), command.companyId()),
                        payment.getId(), null, null, null);
            }
            case CREDIT_NOTE -> {
                if (command.sourceDocumentId() == null)
                    throw new IllegalArgumentException(
                            "sourceDocumentId is required for a CREDIT_NOTE source");
                // Ya quedo bloqueada arriba, junto con la factura destino y en orden de id.
                BillingDocumentRef creditNote = resolveDocument(command.sourceDocumentId(),
                        command.companyId());
                yield ResolvedSource.pointingTo(ApplicationSourceKind.CREDIT_NOTE, creditNote.id(),
                        creditNote.totalAmount(), applicationRepository
                                .sumAppliedFromSourceDocument(creditNote.id(), command.companyId()),
                        null, creditNote, null, null);
            }
            // WITHHOLDING. La retencion tiene que ser la de ESTA factura, y esa
            // comprobacion no es formalismo: sin ella se podria saldar la factura de
            // septiembre con la retencion practicada sobre la de agosto -la cartera
            // cuadraria y la declaracion no, y el descuadre aparece un ano despues-.
            case WITHHOLDING -> {
                if (command.withholdingId() == null)
                    throw new IllegalArgumentException(
                            "withholdingId is required for a WITHHOLDING source");
                WithholdingRef withholding = withholdingQueryPort
                        .findByIdAndCompanyId(command.withholdingId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Withholding not found: " + command.withholdingId()));
                if (!withholding.esDelDocumento(command.targetDocumentId()))
                    throw new IllegalArgumentException("La retencion " + withholding.id()
                            + " se practico sobre el documento " + withholding.billingDocumentId()
                            + " y no sobre el " + command.targetDocumentId()
                            + ": una retencion solo salda la factura sobre la que se practico");
                yield ResolvedSource.pointingTo(ApplicationSourceKind.WITHHOLDING, withholding.id(),
                        withholding.amount(), applicationRepository
                                .sumAppliedFromWithholding(withholding.id(), command.companyId()),
                        null, null, withholding.id(), null);
            }
            // CUSTOMER_CREDIT. Se consume de un LOTE, nunca de "el saldo": de una suma
            // no se puede decir cual parte caduca antes, y sin eso la caducidad del
            // saldo a favor no es calculable.
            case CUSTOMER_CREDIT -> {
                if (command.creditEntryId() == null)
                    throw new IllegalArgumentException(
                            "creditEntryId is required for a CUSTOMER_CREDIT source");
                // El candado del LOTE, y no el de ninguna factura, es lo unico que
                // serializa este origen: el mismo lote se puede aplicar a dos facturas
                // distintas, y esas dos transacciones bloquean documentos distintos.
                CustomerCreditLotRef lot = customerCreditQueryPort
                        .lockLotByIdAndCompanyId(command.creditEntryId(), command.companyId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Customer credit lot not found: " + command.creditEntryId()));
                // Un lote caducado ya dio de baja su remanente con su propio asiento;
                // aplicarlo ahora seria gastar dos veces el mismo dinero.
                if (lot.haCaducado(LocalDate.now(clock)))
                    throw new IllegalArgumentException("El lote de saldo a favor " + lot.id()
                            + " caduco el " + lot.expiresOn() + " y ya no se puede aplicar");
                yield ResolvedSource
                        .pointingTo(ApplicationSourceKind.CUSTOMER_CREDIT, lot.id(),
                                lot.grantedAmount(), applicationRepository
                                        .sumAppliedFromCreditEntry(lot.id(), command.companyId()),
                                null, null, null, lot.id());
            }
            // ROUNDING y WRITE_OFF no apuntan a ninguna fila: no hay nada que resolver
            // ni que bloquear mas alla del documento destino, que ya quedo bloqueado.
            // Su techo es el SALDO PENDIENTE de la factura -castigar o redondear mas de
            // lo que se debe la dejaria con saldo negativo-, y sus barandillas propias
            // -el tope de tres pesos y la firma nominal- las impone el dominio al
            // construir la fila, no aqui.
            case ROUNDING, WRITE_OFF -> {
                BillingDocumentRef documento = resolveDocument(command.targetDocumentId(),
                        command.companyId());
                yield ResolvedSource.pointingTo(command.sourceKind(), documento.id(),
                        saldoPendiente(documento), BigDecimal.ZERO, null, null, null, null);
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
            // La retencion: salda sin que entre un peso, y su fecha VALOR es la del dia
            // en que se practico, no la del dia en que alguien la registro.
            case WITHHOLDING -> BillingDocumentApplication.fromWithholding(command.companyId(),
                    target, source.withholdingId(), command.appliedAmount(),
                    command.clientRequestId(), appliedAt, fechaValor(command, appliedAt));
            case CUSTOMER_CREDIT -> BillingDocumentApplication.fromCustomerCredit(
                    command.companyId(), target, source.creditEntryId(), command.appliedAmount(),
                    command.clientRequestId(), appliedAt, fechaValor(command, appliedAt));
            // El tope de tres pesos lo impone el constructor del dominio, no este
            // metodo: asi tambien lo cumple cualquier otro camino que construya la fila.
            case ROUNDING -> BillingDocumentApplication.fromRounding(command.companyId(), target,
                    command.appliedAmount(), command.clientRequestId(), appliedAt,
                    fechaValor(command, appliedAt));
            // La firma nominal viene del principal, puesta por el controller. Si falta,
            // el dominio lanza WriteOffSignatureRequiredException y no se escribe nada.
            case WRITE_OFF -> BillingDocumentApplication.fromWriteOff(command.companyId(), target,
                    command.appliedAmount(), command.writeOffAuthorizedBySystemUserId(),
                    command.writeOffReason(), command.clientRequestId(), appliedAt,
                    fechaValor(command, appliedAt));
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
     * La fecha valor pedida, o el dia de la aplicacion si no llega ninguna.
     *
     * <p>
     * El defecto es correcto para un pago y una nota credito -se aplican el dia que
     * ocurren- y por eso no es obligatoria en el cuerpo. En una retencion
     * practicada el 30 de octubre y registrada el 3 de noviembre, en cambio,
     * dejarla al defecto la metaria en la declaracion de noviembre.
     */
    private static LocalDate fechaValor(ApplyBillingDocumentCommand command,
            LocalDateTime appliedAt) {
        return command.valueDate() == null ? appliedAt.toLocalDate() : command.valueDate();
    }

    /**
     * El saldo pendiente del documento, que es el techo de los dos origenes sin
     * fila de origen.
     *
     * <p>
     * {@code balanceAmount} es una columna calculada que mantiene la base; cuando
     * el {@code Ref} no la trae se cae al total, que es el techo mas conservador
     * posible -nunca deja aplicar de mas, como mucho deja aplicar de menos-.
     */
    private static BigDecimal saldoPendiente(BillingDocumentRef documento) {
        return documento.balanceAmount() == null
                ? documento.totalAmount()
                : documento.balanceAmount();
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
