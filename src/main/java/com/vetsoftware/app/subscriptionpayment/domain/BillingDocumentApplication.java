package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Que salda que. Existe separada de {@link SubscriptionPayment} por dos
 * razones, y la segunda es la que se olvido en la primera version del modelo:
 *
 * <ol>
 * <li>Un pago puede saldar tres facturas, y una factura puede saldarse con tres
 * pagos. La relacion es muchos a muchos y no cabe en una columna.
 * <li><strong>No todo lo que salda una factura es un pago.</strong> Un saldo a
 * favor de una nota credito tambien la reduce, sin que entre un peso. Cuando
 * esta tabla solo aceptaba pagos, el saldo no bajaba nunca, el reloj de la mora
 * seguia corriendo, y una clinica a la que se le habia devuelto dinero acababa
 * en solo lectura por una deuda que ya no existia.
 * </ol>
 *
 * <h2>Los seis origenes, y por que cuatro de ellos no son un lujo</h2>
 *
 * <p>
 * {@link ApplicationSourceKind#PAYMENT} y
 * {@link ApplicationSourceKind#CREDIT_NOTE} eran los dos unicos que esta clase
 * sabia escribir. Los otros cuatro estaban en el esquema y se rechazaban por
 * nombre, con estas consecuencias:
 *
 * <ul>
 * <li><b>{@link ApplicationSourceKind#WITHHOLDING}</b> — el peor. Ana debe
 * 213.010, su contadora le practica 7.160 de retencion y le gira 205.850. Sin
 * camino para la retencion quedan 7.160 vivos, pasan los cinco dias de gracia y
 * la cuenta cae a <b>solo lectura por una deuda que fiscalmente no existe</b>:
 * ese dinero esta en la DIAN a nombre de Lumbre y Ana tiene el certificado que
 * lo prueba. El sistema tenia razon segun sus propios numeros, y esa es
 * justamente la parte que nadie iba a mirar.
 * <li><b>{@link ApplicationSourceKind#CUSTOMER_CREDIT}</b> — el saldo a favor
 * nacia y no podia aplicarse nunca. Existia y no servia.
 * <li><b>{@link ApplicationSourceKind#ROUNDING}</b> — dos pesos de diferencia
 * entre el impuesto agregado de aqui y el que calculo linea a linea el emisor
 * externo dejaban la factura viva y arrancaban la mora.
 * <li><b>{@link ApplicationSourceKind#WRITE_OFF}</b> — una deuda incobrable
 * solo se podia quitar borrandola, que es justo lo que el modelo prohibe.
 * </ul>
 *
 * <h2>Las cuatro barandillas que el esquema impone y este dominio repite</h2>
 *
 * <ol>
 * <li><b>El origen es excluyente</b> ({@code chk_bda_source_exclusive}): se
 * rellena el campo de <em>su</em> origen y ninguno de los otros tres. Sin esto,
 * una fila podria decir que es una retencion y apuntar ademas a un pago, que es
 * dinero contado dos veces.
 * <li><b>El redondeo tiene tope duro</b> ({@code chk_bda_rounding_cap}), para
 * que no se convierta en el vertedero donde cuadra cualquier descuadre.
 * <li><b>El castigo exige autorizante y motivo</b>
 * ({@code chk_bda_write_off_signature}).
 * <li><b>Lo aplicado desde un origen no puede pasarse del origen</b> — R3, que
 * no cabe aqui porque necesita sumar filas: la comprueba el caso de uso.
 * </ol>
 *
 * <p>
 * <strong>Aplicar mueve dinero, asi que lleva llave de idempotencia</strong>
 * (R13). No basta con R3: aquella acota <em>cuanto</em> se ha aplicado desde un
 * origen, no <em>cuantas veces</em>. Con un pago de 100 y dos peticiones
 * identicas de 50, el total aplicado es 100 y R3 lo da por bueno — pero el
 * operador quiso aplicar 50 una vez y la factura queda saldada por el doble.
 *
 * <p>
 * <strong>Nada se edita ni se borra.</strong> Una aplicacion equivocada se
 * deshace creando otra que la contra-aplica ({@link #reversalOf}), y las dos
 * quedan. Por eso la clase no tiene un solo mutador y por eso
 * {@code billing_document_applications} va exenta de {@code @Version} con el
 * codigo {@code E1_APPEND_ONLY}.
 */
public class BillingDocumentApplication {

    /** Ancho de {@code billing_document_applications.client_request_id}. */
    private static final int MAX_CLIENT_REQUEST_ID_LENGTH = 64;

    /** Ancho de {@code billing_document_applications.write_off_reason}. */
    private static final int MAX_WRITE_OFF_REASON_LENGTH = 255;

    /**
     * Tope duro del residuo de redondeo, espejo de {@code chk_bda_rounding_cap}.
     *
     * <p>
     * Tres pesos, y el numero no es negociable desde el codigo: es el mismo que la
     * base comprueba, asi que subirlo aqui sin subirlo alli cambia un 400 explicado
     * por un 500 de constraint a mitad de una operacion de dinero.
     */
    public static final BigDecimal MAX_ROUNDING_ABS = new BigDecimal("3");

    private final Long id;
    private final Long companyId;
    private final BillingDocumentRef targetDocument;
    private final ApplicationSourceKind sourceKind;

    /** Origen cuando es un pago. Excluyente con los otros tres. */
    private final Long paymentId;

    /** Origen cuando es una nota credito. Excluyente con los otros tres. */
    private final BillingDocumentRef sourceDocument;

    /**
     * Origen cuando es una retencion: la fila de {@code document_withholdings} con
     * el detalle fiscal, que <b>no se copia aqui</b>.
     */
    private final Long withholdingId;

    /**
     * Origen cuando es saldo a favor: el lote de {@code customer_credit_entries}
     * del que sale. Es el lote y no "el saldo", porque el saldo es una suma y de
     * una suma no se puede decir cual caduca antes.
     */
    private final Long creditEntryId;

    /** Con signo: positivo si aplica, negativo si contra-aplica. */
    private final BigDecimal appliedAmount;

    private final Long reversalOfId;

    /**
     * Quien autorizo el castigo. <b>Solo en {@code WRITE_OFF} y obligatorio
     * ahi.</b> Lo pone el backend desde el principal, nunca el cuerpo de la
     * peticion.
     */
    private final Long writeOffAuthorizedBySystemUserId;

    /** El motivo del castigo. Solo en {@code WRITE_OFF} y obligatorio ahi. */
    private final String writeOffReason;

    /**
     * Llave de idempotencia del cliente (R13). Es {@code null} en las reversas, que
     * se deduplican por {@code uq_bda_reversal}.
     */
    private final String clientRequestId;

    private final LocalDateTime appliedAt;

    /**
     * <b>Cuando el asiento CUENTA, que no es cuando se registro.</b> Una retencion
     * practicada el 30 de octubre y registrada el 3 de noviembre pertenece a
     * octubre, y esa diferencia es la que decide en que periodo cae el hecho. En un
     * pago o una nota credito coinciden; en los cuatro origenes nuevos no tienen
     * por que.
     */
    private final LocalDate valueDate;

    private final LocalDateTime createdDate;

    /** Constructor canonico: todos los campos, incluidos los cuatro origenes. */
    public BillingDocumentApplication(Long id, Long companyId, BillingDocumentRef targetDocument,
            ApplicationSourceKind sourceKind, Long paymentId, BillingDocumentRef sourceDocument,
            Long withholdingId, Long creditEntryId, BigDecimal appliedAmount, Long reversalOfId,
            Long writeOffAuthorizedBySystemUserId, String writeOffReason, String clientRequestId,
            LocalDateTime appliedAt, LocalDate valueDate, LocalDateTime createdDate) {
        validate(companyId, targetDocument, sourceKind, paymentId, sourceDocument, withholdingId,
                creditEntryId, appliedAmount, reversalOfId, writeOffAuthorizedBySystemUserId,
                writeOffReason, clientRequestId, appliedAt, valueDate);
        this.id = id;
        this.companyId = companyId;
        this.targetDocument = targetDocument;
        this.sourceKind = sourceKind;
        this.paymentId = paymentId;
        this.sourceDocument = sourceDocument;
        this.withholdingId = withholdingId;
        this.creditEntryId = creditEntryId;
        this.appliedAmount = appliedAmount;
        this.reversalOfId = reversalOfId;
        this.writeOffAuthorizedBySystemUserId = writeOffAuthorizedBySystemUserId;
        this.writeOffReason = writeOffReason;
        this.clientRequestId = clientRequestId;
        this.appliedAt = appliedAt;
        this.valueDate = valueDate;
        this.createdDate = createdDate;
    }

    /**
     * Forma corta para los dos origenes que no tienen ni referencia nueva ni fecha
     * valor distinta.
     *
     * <p>
     * Se mantiene <b>a proposito</b> en vez de propagar cuatro parametros nulos por
     * todos los llamadores: un pago y una nota credito se aplican el dia que
     * ocurren, asi que su {@code value_date} <em>es</em> el dia de
     * {@code appliedAt}. Escribirlo aqui una vez es mejor que escribirlo en cada
     * sitio que construya uno.
     */
    public BillingDocumentApplication(Long id, Long companyId, BillingDocumentRef targetDocument,
            ApplicationSourceKind sourceKind, Long paymentId, BillingDocumentRef sourceDocument,
            BigDecimal appliedAmount, Long reversalOfId, String clientRequestId,
            LocalDateTime appliedAt, LocalDateTime createdDate) {
        this(id, companyId, targetDocument, sourceKind, paymentId, sourceDocument, null, null,
                appliedAmount, reversalOfId, null, null, clientRequestId, appliedAt,
                appliedAt == null ? null : appliedAt.toLocalDate(), createdDate);
    }

    /** Aplicacion de un pago recibido contra una factura. */
    public static BillingDocumentApplication fromPayment(Long companyId,
            BillingDocumentRef targetDocument, Long paymentId, BigDecimal appliedAmount,
            String clientRequestId, LocalDateTime appliedAt) {
        return new BillingDocumentApplication(null, companyId, targetDocument,
                ApplicationSourceKind.PAYMENT, paymentId, null, appliedAmount, null,
                clientRequestId, appliedAt, appliedAt);
    }

    /**
     * Aplicacion de un saldo a favor. <strong>No entra un peso y la factura queda
     * saldada igual</strong>: es el caso que el modelo original no podia
     * representar.
     */
    public static BillingDocumentApplication fromCreditNote(Long companyId,
            BillingDocumentRef targetDocument, BillingDocumentRef creditNote,
            BigDecimal appliedAmount, String clientRequestId, LocalDateTime appliedAt) {
        if (creditNote != null && !creditNote.isCreditNote())
            throw new IllegalArgumentException(
                    "source document must be a CREDIT_NOTE, got " + creditNote.documentKind());
        return new BillingDocumentApplication(null, companyId, targetDocument,
                ApplicationSourceKind.CREDIT_NOTE, null, creditNote, appliedAmount, null,
                clientRequestId, appliedAt, appliedAt);
    }

    /**
     * <b>La retencion que salda.</b> No es un descuento ni un impago: es plata del
     * cliente que fue directa a la DIAN, asi que la factura queda saldada por ese
     * importe aunque el dinero no haya entrado a la caja.
     *
     * @param valueDate
     *            el dia en que la retencion se practico, no el dia en que se
     *            registro: es lo que decide en que declaracion cae
     */
    public static BillingDocumentApplication fromWithholding(Long companyId,
            BillingDocumentRef targetDocument, Long withholdingId, BigDecimal appliedAmount,
            String clientRequestId, LocalDateTime appliedAt, LocalDate valueDate) {
        return new BillingDocumentApplication(null, companyId, targetDocument,
                ApplicationSourceKind.WITHHOLDING, null, null, withholdingId, null, appliedAmount,
                null, null, null, clientRequestId, appliedAt, valueDate, appliedAt);
    }

    /** Consumo de un lote de saldo a favor contra una factura. */
    public static BillingDocumentApplication fromCustomerCredit(Long companyId,
            BillingDocumentRef targetDocument, Long creditEntryId, BigDecimal appliedAmount,
            String clientRequestId, LocalDateTime appliedAt, LocalDate valueDate) {
        return new BillingDocumentApplication(null, companyId, targetDocument,
                ApplicationSourceKind.CUSTOMER_CREDIT, null, null, null, creditEntryId,
                appliedAmount, null, null, null, clientRequestId, appliedAt, valueDate, appliedAt);
    }

    /**
     * El residuo de uno a tres pesos que ningun medio de pago mueve.
     *
     * <p>
     * <b>Sin referencia de origen y con tope duro.</b> No hay ninguna fila detras
     * de un redondeo —no la hay que buscar— y por eso el tope es la unica
     * barandilla: es lo que impide que este origen se coma un descuadre de verdad.
     */
    public static BillingDocumentApplication fromRounding(Long companyId,
            BillingDocumentRef targetDocument, BigDecimal appliedAmount, String clientRequestId,
            LocalDateTime appliedAt, LocalDate valueDate) {
        return new BillingDocumentApplication(null, companyId, targetDocument,
                ApplicationSourceKind.ROUNDING, null, null, null, null, appliedAmount, null, null,
                null, clientRequestId, appliedAt, valueDate, appliedAt);
    }

    /**
     * El castigo de una deuda incobrable.
     *
     * <p>
     * <b>La deuda no se borra: se agrega la fila que la da de baja, y las dos
     * quedan.</b> Exige firma nominal de plataforma y motivo escrito, que es lo
     * unico que hace auditable una operacion que hace desaparecer dinero.
     */
    public static BillingDocumentApplication fromWriteOff(Long companyId,
            BillingDocumentRef targetDocument, BigDecimal appliedAmount,
            Long authorizedBySystemUserId, String reason, String clientRequestId,
            LocalDateTime appliedAt, LocalDate valueDate) {
        return new BillingDocumentApplication(null, companyId, targetDocument,
                ApplicationSourceKind.WRITE_OFF, null, null, null, null, appliedAmount, null,
                authorizedBySystemUserId, reason, clientRequestId, appliedAt, valueDate, appliedAt);
    }

    /**
     * Contra-aplicacion de una aplicacion existente: misma factura, mismo origen,
     * importe negado y un puntero a la original.
     *
     * <p>
     * <strong>Esto es lo que sustituye al {@code UPDATE} y al {@code DELETE} que
     * este modelo no admite.</strong> La original se queda donde esta, la reversa
     * la neutraliza, y la suma neta de las dos es cero, que es lo que libera el
     * importe del origen para volver a aplicarlo.
     *
     * <p>
     * <strong>La reversa copia la referencia del origen, incluidos los cuatro
     * nuevos.</strong> Tiene que hacerlo: {@code chk_bda_source_exclusive} exige
     * que una fila {@code WITHHOLDING} apunte a su retencion, reversa o no, y
     * ademas es lo que permite que la suma neta por origen siga cuadrando. La firma
     * del castigo tambien viaja, porque {@code chk_bda_write_off_signature} la
     * exige en toda fila de ese tipo — y porque deshacer un castigo sin dejar dicho
     * de quien era la firma que se deshace no es una auditoria.
     *
     * <p>
     * <strong>La reversa no hereda la llave de idempotencia de la
     * original</strong>: esa llave ya esta tomada por la fila que se contra-aplica,
     * asi que copiarla chocaria contra su indice unico. La reversa tiene la suya
     * propia y mejor: {@code uq_bda_reversal} solo admite una por original.
     */
    public static BillingDocumentApplication reversalOf(BillingDocumentApplication original,
            LocalDateTime appliedAt) {
        if (original == null)
            throw new IllegalArgumentException("original application is required");
        // La comprobacion intrinseca va ANTES que la de estado, y el orden importa
        // para quien lee el error: si a una reversa sin persistir se le dice
        // «must be persisted», el operador la guarda y vuelve a fallar. Que una
        // reversa no se pueda revertir no se arregla persistiendola: no se arregla.
        if (original.isReversal())
            throw new IllegalArgumentException("a reversal cannot be reversed");
        if (original.getId() == null)
            throw new IllegalArgumentException("original application must be persisted");
        return new BillingDocumentApplication(null, original.getCompanyId(),
                original.getTargetDocument(), original.getSourceKind(), original.getPaymentId(),
                original.getSourceDocument(), original.getWithholdingId(),
                original.getCreditEntryId(), original.getAppliedAmount().negate(), original.getId(),
                original.getWriteOffAuthorizedBySystemUserId(), original.getWriteOffReason(), null,
                appliedAt, appliedAt == null ? null : appliedAt.toLocalDate(), appliedAt);
    }

    public boolean isReversal() {
        return reversalOfId != null;
    }

    private static void validate(Long companyId, BillingDocumentRef targetDocument,
            ApplicationSourceKind sourceKind, Long paymentId, BillingDocumentRef sourceDocument,
            Long withholdingId, Long creditEntryId, BigDecimal appliedAmount, Long reversalOfId,
            Long writeOffAuthorizedBySystemUserId, String writeOffReason, String clientRequestId,
            LocalDateTime appliedAt, LocalDate valueDate) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (targetDocument == null)
            throw new IllegalArgumentException("targetDocument is required");
        // La FK compuesta (company_id, target_document_id) ya lo impide en la base.
        // Se repite aqui para que el rechazo llegue como un 400 con mensaje y no como
        // una violacion de integridad al hacer flush, y para que un test unitario
        // pueda ejercitarlo sin base de datos.
        if (!companyId.equals(targetDocument.companyId()))
            throw new IllegalArgumentException(
                    "targetDocument belongs to another company: " + targetDocument.companyId());
        if (sourceKind == null)
            throw new IllegalArgumentException("sourceKind is required");
        validarOrigenExcluyente(companyId, targetDocument, sourceKind, paymentId, sourceDocument,
                withholdingId, creditEntryId);
        validarImporte(sourceKind, appliedAmount, reversalOfId);
        validarFirmaDelCastigo(sourceKind, writeOffAuthorizedBySystemUserId, writeOffReason);
        if (clientRequestId != null && clientRequestId.length() > MAX_CLIENT_REQUEST_ID_LENGTH)
            throw new IllegalArgumentException("clientRequestId must be 64 chars or less");
        if (appliedAt == null)
            throw new IllegalArgumentException("appliedAt is required");
        // value_date nace NOT NULL en el changeset 253. Sin ella el asiento no cae en
        // ningun periodo, y un cobro que no cae en un periodo no se puede cerrar.
        if (valueDate == null)
            throw new IllegalArgumentException("valueDate is required");
    }

    /**
     * Espejo de {@code chk_bda_source_exclusive}: <b>uno u otro, nunca los dos y
     * nunca ninguno</b>. Sin esto el saldo se reduce sin saber de donde salio el
     * dinero, y una fila podria afirmar dos origenes a la vez —que es contar el
     * mismo dinero dos veces—.
     */
    private static void validarOrigenExcluyente(Long companyId, BillingDocumentRef targetDocument,
            ApplicationSourceKind sourceKind, Long paymentId, BillingDocumentRef sourceDocument,
            Long withholdingId, Long creditEntryId) {
        switch (sourceKind) {
            case PAYMENT -> {
                exigirPresente(paymentId, "paymentId", sourceKind);
                prohibir(sourceDocument, "sourceDocument", sourceKind);
                prohibir(withholdingId, "withholdingId", sourceKind);
                prohibir(creditEntryId, "creditEntryId", sourceKind);
            }
            case CREDIT_NOTE -> {
                exigirPresente(sourceDocument, "sourceDocument", sourceKind);
                prohibir(paymentId, "paymentId", sourceKind);
                prohibir(withholdingId, "withholdingId", sourceKind);
                prohibir(creditEntryId, "creditEntryId", sourceKind);
                if (!companyId.equals(sourceDocument.companyId()))
                    throw new IllegalArgumentException("sourceDocument belongs to another company: "
                            + sourceDocument.companyId());
                if (sourceDocument.id().equals(targetDocument.id()))
                    throw new IllegalArgumentException("a document cannot settle itself");
            }
            case WITHHOLDING -> {
                exigirPresente(withholdingId, "withholdingId", sourceKind);
                prohibir(paymentId, "paymentId", sourceKind);
                prohibir(sourceDocument, "sourceDocument", sourceKind);
                prohibir(creditEntryId, "creditEntryId", sourceKind);
            }
            case CUSTOMER_CREDIT -> {
                exigirPresente(creditEntryId, "creditEntryId", sourceKind);
                prohibir(paymentId, "paymentId", sourceKind);
                prohibir(sourceDocument, "sourceDocument", sourceKind);
                prohibir(withholdingId, "withholdingId", sourceKind);
            }
            // Los dos que no apuntan a nada: no hay fila de origen que buscar. Su
            // barandilla es el tope (ROUNDING) y la firma (WRITE_OFF).
            case ROUNDING, WRITE_OFF -> {
                prohibir(paymentId, "paymentId", sourceKind);
                prohibir(sourceDocument, "sourceDocument", sourceKind);
                prohibir(withholdingId, "withholdingId", sourceKind);
                prohibir(creditEntryId, "creditEntryId", sourceKind);
            }
        }
    }

    private static void exigirPresente(Object value, String field, ApplicationSourceKind kind) {
        if (value == null)
            throw new IllegalArgumentException(field + " is required for a " + kind + " source");
    }

    private static void prohibir(Object value, String field, ApplicationSourceKind kind) {
        if (value != null)
            throw new IllegalArgumentException(field + " must be null for a " + kind + " source");
    }

    /**
     * El importe, sus signos y el tope del redondeo.
     *
     * <p>
     * Espejo de {@code chk_bda_amount_not_zero}, {@code chk_bda_reversal_sign} y
     * {@code chk_bda_rounding_cap}. El tope se mide sobre el <b>valor absoluto</b>
     * porque una reversa de redondeo es negativa y sigue estando acotada: si no, se
     * podria contra-aplicar un residuo de menos tres millones.
     */
    private static void validarImporte(ApplicationSourceKind sourceKind, BigDecimal appliedAmount,
            Long reversalOfId) {
        if (appliedAmount == null)
            throw new IllegalArgumentException("appliedAmount is required");
        if (appliedAmount.signum() == 0)
            throw new IllegalArgumentException("appliedAmount cannot be zero");
        // Espejo de chk_bda_reversal_sign: una reversa es negativa y una aplicacion
        // normal positiva. Junto con uq_bda_reversal, esto hace que la suma de
        // applied_amount de un origen sea el neto real y no se pueda inflar.
        if (reversalOfId == null && appliedAmount.signum() < 0)
            throw new IllegalArgumentException("a plain application must be positive");
        if (reversalOfId != null && appliedAmount.signum() > 0)
            throw new IllegalArgumentException("a reversal must be negative");
        if (sourceKind == ApplicationSourceKind.ROUNDING
                && appliedAmount.abs().compareTo(MAX_ROUNDING_ABS) > 0)
            throw new RoundingCapExceededException(appliedAmount, MAX_ROUNDING_ABS);
    }

    /**
     * Espejo de {@code chk_bda_write_off_signature}: los dos campos <b>si y solo
     * si</b> el origen es un castigo. Ponerlos en otra fila afirmaria un hecho
     * falso; no ponerlos en un castigo deja dinero borrado sin nadie detras.
     */
    private static void validarFirmaDelCastigo(ApplicationSourceKind sourceKind,
            Long writeOffAuthorizedBySystemUserId, String writeOffReason) {
        if (sourceKind != ApplicationSourceKind.WRITE_OFF) {
            if (writeOffAuthorizedBySystemUserId != null || writeOffReason != null)
                throw new IllegalArgumentException(
                        "only a WRITE_OFF application carries an authorizer and a reason, but this"
                                + " one is " + sourceKind);
            return;
        }
        if (writeOffAuthorizedBySystemUserId == null)
            throw new WriteOffSignatureRequiredException("falta el usuario de plataforma que lo"
                    + " autoriza, y lo pone el backend desde el principal, nunca la peticion");
        if (writeOffReason == null || writeOffReason.isBlank())
            throw new WriteOffSignatureRequiredException("falta el motivo escrito");
        if (writeOffReason.length() > MAX_WRITE_OFF_REASON_LENGTH)
            throw new WriteOffSignatureRequiredException("el motivo no puede superar los "
                    + MAX_WRITE_OFF_REASON_LENGTH + " caracteres");
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public BillingDocumentRef getTargetDocument() {
        return targetDocument;
    }

    public ApplicationSourceKind getSourceKind() {
        return sourceKind;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public BillingDocumentRef getSourceDocument() {
        return sourceDocument;
    }

    public Long getWithholdingId() {
        return withholdingId;
    }

    public Long getCreditEntryId() {
        return creditEntryId;
    }

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public Long getReversalOfId() {
        return reversalOfId;
    }

    public Long getWriteOffAuthorizedBySystemUserId() {
        return writeOffAuthorizedBySystemUserId;
    }

    public String getWriteOffReason() {
        return writeOffReason;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
