package com.vetsoftware.app.subscriptionbilling.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * La cuenta de cobro que Lumbre calcula y numera ({@code DC-}), con su desglose
 * fiscal y la referencia de la factura que se emitió fuera.
 *
 * <p>
 * <b>No se edita ni se borra.</b> No hay mutador de {@code subtotalAmount},
 * {@code taxAmount}, {@code totalAmount}, {@code period} ni
 * {@code documentKind}: son {@code final}. Los tres únicos campos que se mueven
 * son {@link #issueStatus}, {@link #settledAmount} y —una sola vez— la
 * referencia externa con su {@link #dueDate}. Un error se arregla con un
 * documento nuevo que lo compensa, encadenado por {@code correctsDocumentId}, y
 * los dos quedan visibles.
 *
 * <p>
 * <b>Los importes son siempre positivos y el signo lo da
 * {@link DocumentKind}</b> ({@code chk_sbd_amounts_positive}). Los cargos que
 * agrupa sí llevan signo; la traducción entre las dos convenciones es el valor
 * absoluto que aplica {@link TaxBreakdown}. Un papel con total negativo no
 * existe: existe una nota crédito por el valor absoluto.
 *
 * <p>
 * <b>El saldo no se escribe nunca.</b> {@code balance_amount} es una columna
 * calculada {@code VIRTUAL} de la base ({@code total_amount - settled_amount});
 * aquí {@link #getBalanceAmount()} la deriva con la misma fórmula y no existe
 * ningún campo ni ningún setter que la pueda desincronizar. Es la columna que
 * decide si una cuenta entra en mora, así que <b>un camino de código capaz de
 * desincronizarla es un camino capaz de suspender a quien ya pagó</b>.
 *
 * <p>
 * <b>Lleva {@code version}</b> (BE-26): es la cabecera del agregado y sobre
 * ella se apoya la exención {@code E1_APPEND_ONLY} de las líneas de impuesto y
 * la {@code E6_YA_PROTEGIDO} de los cargos.
 */
public final class SubscriptionBillingDocument {

    private final Long id;
    private final String documentNumber;
    private final Long companyId;
    private final Long subscriptionId;
    private final DocumentKind documentKind;
    private final BillingReason billingReason;
    private final ServicePeriod period;
    private final Long correctsDocumentId;
    private final BigDecimal subtotalAmount;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;
    private final List<BillingDocumentTax> taxes;
    private final LocalDateTime createdDate;

    private IssueStatus issueStatus;
    private ExternalInvoiceReference external;
    private LocalDate dueDate;
    private BigDecimal settledAmount;
    private Long version;

    public SubscriptionBillingDocument(Long id, String documentNumber, Long companyId,
            Long subscriptionId, DocumentKind documentKind, BillingReason billingReason,
            ServicePeriod period, IssueStatus issueStatus, ExternalInvoiceReference external,
            Long correctsDocumentId, LocalDate dueDate, BigDecimal subtotalAmount,
            BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal settledAmount,
            List<BillingDocumentTax> taxes, LocalDateTime createdDate, Long version) {
        if (documentNumber == null || documentNumber.isBlank())
            throw new IllegalArgumentException("documentNumber is required");
        if (documentNumber.length() > DocumentNumber.MAX_LENGTH)
            throw new IllegalArgumentException(
                    "documentNumber must be " + DocumentNumber.MAX_LENGTH + " chars or less");
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (subscriptionId == null)
            throw new IllegalArgumentException("subscriptionId is required");
        if (documentKind == null)
            throw new IllegalArgumentException("documentKind is required");
        if (billingReason == null)
            throw new IllegalArgumentException("billingReason is required");
        if (period == null)
            throw new IllegalArgumentException("period is required");
        if (issueStatus == null)
            throw new IllegalArgumentException("issueStatus is required");
        validarImportes(subtotalAmount, taxAmount, totalAmount, settledAmount);
        validarCorreccion(documentKind, correctsDocumentId, id);
        validarRegistroExterno(issueStatus, external);
        validarVencimiento(dueDate, external);
        this.id = id;
        this.documentNumber = documentNumber;
        this.companyId = companyId;
        this.subscriptionId = subscriptionId;
        this.documentKind = documentKind;
        this.billingReason = billingReason;
        this.period = period;
        this.issueStatus = issueStatus;
        this.external = external;
        this.correctsDocumentId = correctsDocumentId;
        this.dueDate = dueDate;
        this.subtotalAmount = subtotalAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.settledAmount = settledAmount;
        this.taxes = taxes == null ? List.of() : List.copyOf(taxes);
        this.createdDate = createdDate;
        this.version = version;
    }

    /**
     * Calcula el documento a partir de su desglose. Nace {@code DRAFT}, sin
     * referencia externa, <b>sin vencimiento</b> y con cero saldado.
     *
     * <p>
     * <b>Sin {@code dueDate} a propósito</b>: el vencimiento se cuenta desde la
     * fecha fiscal y aquí todavía no existe ninguna. Ver
     * {@link #registerExternalInvoice}.
     */
    public static SubscriptionBillingDocument issue(DocumentNumber documentNumber, Long companyId,
            Long subscriptionId, DocumentKind documentKind, BillingReason billingReason,
            ServicePeriod period, TaxBreakdown breakdown, Long correctsDocumentId, Clock clock) {
        if (breakdown == null)
            throw new IllegalArgumentException("tax breakdown is required");
        LocalDateTime ahora = LocalDateTime.now(clock);
        return new SubscriptionBillingDocument(null, documentNumber.formatted(), companyId,
                subscriptionId, documentKind, billingReason, period, IssueStatus.DRAFT, null,
                correctsDocumentId, null, breakdown.subtotalAmount(), breakdown.taxAmount(),
                breakdown.totalAmount(), Money.zero(), breakdown.lineas(), ahora, null);
    }

    /**
     * {@code DRAFT → AWAITING_EXTERNAL}: el documento queda en la cola de emisión
     * externa. Es el estado cuya antigüedad hay que vigilar — cada fila atascada
     * aquí es dinero devengado que nadie facturó.
     */
    public void submitForExternalIssue() {
        exigirImporteNoSellado("be submitted for external issue");
        if (issueStatus == IssueStatus.VOIDED)
            throw new BillingDocumentAlreadyVoidedException(id);
        if (issueStatus != IssueStatus.DRAFT)
            throw new IllegalStateException(
                    "only a DRAFT document can be submitted, but this one is " + issueStatus);
        this.issueStatus = IssueStatus.AWAITING_EXTERNAL;
    }

    /**
     * Captura la referencia de la factura emitida fuera y, con ella, <b>el
     * vencimiento</b>.
     *
     * <p>
     * <b>El vencimiento se cuenta desde {@code external_issued_at}, la fecha
     * fiscal</b>, nunca desde que se calculó el cobro aquí. Si el documento se
     * calculó el día 1 y la factura se emitió fuera el día 20, un plazo de 15 días
     * medido desde el cálculo pondría la cuenta en mora cinco días antes de que el
     * cliente tuviera nada que pagar: sería suspenderlo por un retraso
     * administrativo propio.
     *
     * <p>
     * Solo se hace <b>una vez</b>. Registrarla de nuevo es cambiar la factura
     * fiscal de un documento que ya la tiene, y ahí es donde Lumbre y la DIAN dejan
     * de coincidir.
     */
    public void registerExternalInvoice(ExternalInvoiceReference reference, int paymentTermDays) {
        if (reference == null)
            throw new IllegalArgumentException("external invoice reference is required");
        if (paymentTermDays < 0)
            throw new IllegalArgumentException("paymentTermDays cannot be negative");
        exigirImporteNoSellado("register another external invoice");
        if (issueStatus == IssueStatus.VOIDED)
            throw new BillingDocumentAlreadyVoidedException(id);
        this.external = reference;
        this.dueDate = reference.fechaDeReferenciaParaElVencimiento().plusDays(paymentTermDays);
        this.issueStatus = IssueStatus.EXTERNAL_REGISTERED;
    }

    /**
     * Anula el documento antes de que exista fuera.
     *
     * <p>
     * <b>Un documento con su factura externa ya registrada no se anula aquí</b>:
     * anularlo dejaría a Lumbre diciendo que ese cobro no existe mientras la DIAN
     * tiene la factura. Se corrige con una nota crédito emitida fuera y registrada
     * aquí, encadenada por {@code correctsDocumentId}.
     */
    public void voidDocument() {
        if (issueStatus == IssueStatus.VOIDED)
            throw new BillingDocumentAlreadyVoidedException(id);
        exigirImporteNoSellado("be voided");
        this.issueStatus = IssueStatus.VOIDED;
    }

    /**
     * Fija lo saldado —pagado o acreditado—. Es el <b>único</b> importe que se
     * mueve después de emitir, y por eso pasa por el camino gestionado con
     * {@code @Version} en vez de por un {@code UPDATE} masivo: ahí un {@code save}
     * concurrente con la versión vieja pisaría el cambio sin excepción y sin log.
     *
     * <p>
     * No se llama «pagado» a propósito: una nota crédito también reduce lo que se
     * debe sin que entre un peso. Espejo de {@code chk_sbd_settled_cap}: saldar más
     * de lo que se debe es como la cartera acaba cuadrando con plata que no existe.
     */
    public void settle(BigDecimal newSettledAmount) {
        if (newSettledAmount == null || newSettledAmount.signum() < 0)
            throw new IllegalArgumentException("settledAmount cannot be negative");
        BigDecimal saldado = Money.scaled(newSettledAmount);
        if (saldado.compareTo(totalAmount) > 0)
            throw new IllegalArgumentException(
                    "settledAmount " + saldado + " exceeds the document total " + totalAmount);
        this.settledAmount = saldado;
    }

    /**
     * El saldo: espejo exacto de la columna calculada
     * {@code balance_amount = total_amount - settled_amount}.
     *
     * <p>
     * <b>Se deriva, no se guarda.</b> No hay campo, no hay setter y el mapper no la
     * escribe: es la única forma de que ningún camino de código pueda
     * desincronizarla de la base. Si alguna vez sientes la tentación de asignarla,
     * es que algo está mal en otro sitio.
     */
    public BigDecimal getBalanceAmount() {
        return Money.scaled(totalAmount.subtract(settledAmount));
    }

    /** {@code true} si la factura externa ya selló el importe (R2). */
    public boolean estaSelladoPorLaFacturaExterna() {
        return issueStatus.estaSelladoPorLaFacturaExterna();
    }

    /**
     * {@code true} si esta fila entra en la barandilla contra la doble facturación:
     * es la factura de un ciclo y no está anulada.
     *
     * <p>
     * Réplica exacta del {@code CASE} de {@code recurring_cycle_marker}. El periodo
     * <b>no</b> entra aquí: va en el índice como columnas reales, que es lo que
     * consigue el «periodo exacto».
     */
    public boolean entraEnLaBarandillaDeCiclo() {
        return documentKind == DocumentKind.INVOICE
                && billingReason == BillingReason.RECURRING_CYCLE
                && issueStatus != IssueStatus.VOIDED;
    }

    private void exigirImporteNoSellado(String intento) {
        if (issueStatus.estaSelladoPorLaFacturaExterna())
            throw new BillingDocumentAlreadyIssuedException(id, intento);
    }

    /**
     * Espejo de {@code chk_sbd_amounts_positive}, {@code chk_sbd_total} y
     * {@code chk_sbd_settled_cap}.
     */
    private static void validarImportes(BigDecimal subtotal, BigDecimal tax, BigDecimal total,
            BigDecimal settled) {
        if (subtotal == null || tax == null || total == null || settled == null)
            throw new IllegalArgumentException("document amounts are required");
        if (subtotal.signum() < 0 || tax.signum() < 0 || total.signum() < 0 || settled.signum() < 0)
            throw new IllegalArgumentException(
                    "document amounts are always positive: the sign is given by the document kind");
        if (total.compareTo(subtotal.add(tax)) != 0)
            throw new IllegalArgumentException(
                    "totalAmount must equal subtotalAmount plus taxAmount");
        if (settled.compareTo(total) > 0)
            throw new IllegalArgumentException("settledAmount cannot exceed totalAmount");
    }

    /**
     * Espejo de {@code chk_sbd_corrects_kind}, más la regla que el motor no puede
     * declarar: un documento no puede corregirse a sí mismo. Un {@code CHECK} no
     * puede referenciar una columna {@code AUTO_INCREMENT}, así que esa mitad baja
     * al código y vive aquí.
     */
    private static void validarCorreccion(DocumentKind kind, Long correctsDocumentId, Long id) {
        if (correctsDocumentId == null)
            return;
        if (!kind.puedeCorregir())
            throw new IllegalArgumentException(
                    "only a credit or debit note can correct another document, but this is "
                            + kind);
        if (id != null && id.equals(correctsDocumentId))
            throw new IllegalArgumentException("a document cannot correct itself: " + id);
    }

    /** Espejo de {@code chk_sbd_external_registered}. */
    private static void validarRegistroExterno(IssueStatus status,
            ExternalInvoiceReference external) {
        if (status == IssueStatus.EXTERNAL_REGISTERED && external == null)
            throw new IllegalArgumentException(
                    "an EXTERNAL_REGISTERED document needs its external invoice number, issue"
                            + " date and provider");
    }

    /** Espejo de {@code chk_sbd_due_date}. */
    private static void validarVencimiento(LocalDate dueDate, ExternalInvoiceReference external) {
        if (dueDate == null || external == null)
            return;
        if (dueDate.isBefore(external.issuedAt()))
            throw new IllegalArgumentException(
                    "dueDate cannot be before the external issue date " + external.issuedAt());
    }

    public Long getId() {
        return id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public DocumentKind getDocumentKind() {
        return documentKind;
    }

    public BillingReason getBillingReason() {
        return billingReason;
    }

    public ServicePeriod getPeriod() {
        return period;
    }

    public IssueStatus getIssueStatus() {
        return issueStatus;
    }

    public ExternalInvoiceReference getExternal() {
        return external;
    }

    public Long getCorrectsDocumentId() {
        return correctsDocumentId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    public List<BillingDocumentTax> getTaxes() {
        return taxes;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }
}
