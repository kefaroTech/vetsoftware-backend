package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;
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
 * <p>
 * <strong>Aplicar mueve dinero, asi que lleva llave de idempotencia</strong>
 * (R13). No basta con R3: aquella acota <em>cuanto</em> se ha aplicado desde un
 * origen, no <em>cuantas veces</em>. Con un pago de 100 y dos peticiones
 * identicas de 50, el total aplicado es 100 y R3 lo da por bueno — pero el
 * operador quiso aplicar 50 una vez y la factura queda saldada por el doble. La
 * llave es lo unico que distingue «dos aplicaciones de 50» de «la misma
 * aplicacion de 50 pedida dos veces».
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

    private final Long id;
    private final Long companyId;
    private final BillingDocumentRef targetDocument;
    private final ApplicationSourceKind sourceKind;

    /** Origen cuando es un pago. Excluyente con {@link #sourceDocument}. */
    private final Long paymentId;

    /** Origen cuando es una nota credito. Excluyente con {@link #paymentId}. */
    private final BillingDocumentRef sourceDocument;

    /** Con signo: positivo si aplica, negativo si contra-aplica. */
    private final BigDecimal appliedAmount;

    private final Long reversalOfId;

    /**
     * Llave de idempotencia del cliente (R13). Es {@code null} en las reversas, que
     * se deduplican por {@code uq_bda_reversal}.
     */
    private final String clientRequestId;

    private final LocalDateTime appliedAt;
    private final LocalDateTime createdDate;

    public BillingDocumentApplication(Long id, Long companyId, BillingDocumentRef targetDocument,
            ApplicationSourceKind sourceKind, Long paymentId, BillingDocumentRef sourceDocument,
            BigDecimal appliedAmount, Long reversalOfId, String clientRequestId,
            LocalDateTime appliedAt, LocalDateTime createdDate) {
        validate(companyId, targetDocument, sourceKind, paymentId, sourceDocument, appliedAmount,
                reversalOfId, clientRequestId, appliedAt);
        this.id = id;
        this.companyId = companyId;
        this.targetDocument = targetDocument;
        this.sourceKind = sourceKind;
        this.paymentId = paymentId;
        this.sourceDocument = sourceDocument;
        this.appliedAmount = appliedAmount;
        this.reversalOfId = reversalOfId;
        this.clientRequestId = clientRequestId;
        this.appliedAt = appliedAt;
        this.createdDate = createdDate;
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
     * <strong>La reversa no hereda la llave de idempotencia de la
     * original</strong>, y no es un descuido: esa llave ya esta tomada por la fila
     * que se esta contra-aplicando, asi que copiarla chocaria contra su indice
     * unico. La reversa tiene la suya propia y mejor: {@code uq_bda_reversal} solo
     * admite una contra-aplicacion por original.
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
                original.getSourceDocument(), original.getAppliedAmount().negate(),
                original.getId(), null, appliedAt, appliedAt);
    }

    public boolean isReversal() {
        return reversalOfId != null;
    }

    private static void validate(Long companyId, BillingDocumentRef targetDocument,
            ApplicationSourceKind sourceKind, Long paymentId, BillingDocumentRef sourceDocument,
            BigDecimal appliedAmount, Long reversalOfId, String clientRequestId,
            LocalDateTime appliedAt) {
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
        // Espejo de chk_bda_source_exclusive: uno u otro, nunca los dos y nunca
        // ninguno. Sin esto el saldo se reduce sin saber de donde salio el dinero.
        switch (sourceKind) {
            case PAYMENT -> {
                if (paymentId == null)
                    throw new IllegalArgumentException(
                            "paymentId is required for a PAYMENT source");
                if (sourceDocument != null)
                    throw new IllegalArgumentException(
                            "sourceDocument must be null for a PAYMENT source");
            }
            case CREDIT_NOTE -> {
                if (sourceDocument == null)
                    throw new IllegalArgumentException(
                            "sourceDocument is required for a CREDIT_NOTE source");
                if (paymentId != null)
                    throw new IllegalArgumentException(
                            "paymentId must be null for a CREDIT_NOTE source");
                if (!companyId.equals(sourceDocument.companyId()))
                    throw new IllegalArgumentException("sourceDocument belongs to another company: "
                            + sourceDocument.companyId());
                if (sourceDocument.id().equals(targetDocument.id()))
                    throw new IllegalArgumentException("a document cannot settle itself");
            }
        }
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
        if (clientRequestId != null && clientRequestId.length() > MAX_CLIENT_REQUEST_ID_LENGTH)
            throw new IllegalArgumentException("clientRequestId must be 64 chars or less");
        if (appliedAt == null)
            throw new IllegalArgumentException("appliedAt is required");
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

    public BigDecimal getAppliedAmount() {
        return appliedAmount;
    }

    public Long getReversalOfId() {
        return reversalOfId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
