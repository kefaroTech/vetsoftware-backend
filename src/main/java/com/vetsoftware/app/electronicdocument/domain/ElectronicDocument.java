package com.vetsoftware.app.electronicdocument.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Documento electronico (cabecera + lineas + pagos): la entidad fiscal INMUTABLE.
 * Se construye congelando una cuenta cerrada (ver {@link #createPending}). En F2 nace PENDIENTE,
 * sin numero (prefix/consecutive) ni sellos DIAN (cufe/cude/uuid/qr/xml/pdf): los llenan F4 y F3.
 * No expone update ni soft-delete: la unica correccion valida es una nota credito/debito (F5).
 */
public class ElectronicDocument {
    private static final int MONEY_SCALE = 2;
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final String COLOMBIA_OFFSET = "-05:00";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Long id;
    private final Long companyId;
    private final Long openAccountId;
    private final ElectronicDocumentType documentType;

    // Campos del ciclo de vida DIAN: NO finales. Los rellena la transmisión (F3) mediante la máquina
    // de estados forward-only (markValidated/markRejected/markContingency). El contenido fiscal (líneas,
    // totales, snapshots) sí es inmutable.
    private String prefix;
    private Long consecutive;
    // Numero de la resolucion DIAN que autoriza el consecutivo. Se asigna al emitir (F4) desde la
    // NumberingResolution activa de la empresa, junto con prefix/consecutive.
    private String resolutionNumber;

    private final LocalDate issueDate;
    private final String issueTime;

    private String cufe;
    private String cude;
    private String uuid;
    private String qrData;
    private String qrUrl;
    private String xmlSigned;
    private String pdfRepresentation;
    private DianStatus dianStatus;
    private LocalDateTime dianValidationDate;

    private final IssuerSnapshot issuer;
    private final CustomerSnapshot customer;

    private final BigDecimal lineExtensionAmount;
    private final BigDecimal taxExclusiveAmount;
    private final BigDecimal taxInclusiveAmount;
    private final BigDecimal payableAmount;

    // F6 - retenciones que el adquiriente (agente retenedor) practica, congeladas. Cero cuando no aplica.
    private final BigDecimal reteFuenteAmount;
    private final BigDecimal reteIvaAmount;
    private final BigDecimal reteIcaAmount;

    private final PaymentForm paymentForm;
    private final LocalDate paymentDueDate;

    private final List<ElectronicDocumentLine> lines;
    private final List<ElectronicDocumentPayment> payments;

    // F5 - correcciones. Solo poblados en notas (NOTA_CREDITO/NOTA_DEBITO): referencia al documento
    // corregido + concepto DIAN (codigo + texto). En facturas son null. `reversed` marca que ESTA
    // factura fue anulada por una nota credito validada (no final: lo estampa la validacion DIAN).
    private final DocumentReference reference;
    private final String noteReasonCode;
    private final String noteReasonText;
    private boolean reversed;

    private final LocalDateTime createdDate;
    private final boolean enabled;

    public ElectronicDocument(Long id, Long companyId, Long openAccountId, ElectronicDocumentType documentType,
                              String prefix, Long consecutive, String resolutionNumber, LocalDate issueDate, String issueTime,
                              String cufe, String cude, String uuid, String qrData, String qrUrl,
                              String xmlSigned, String pdfRepresentation, DianStatus dianStatus,
                              LocalDateTime dianValidationDate, IssuerSnapshot issuer, CustomerSnapshot customer,
                              BigDecimal lineExtensionAmount, BigDecimal taxExclusiveAmount,
                              BigDecimal taxInclusiveAmount, BigDecimal payableAmount, PaymentForm paymentForm,
                              LocalDate paymentDueDate, List<ElectronicDocumentLine> lines,
                              List<ElectronicDocumentPayment> payments, LocalDateTime createdDate, boolean enabled,
                              DocumentReference reference, String noteReasonCode, String noteReasonText,
                              boolean reversed, BigDecimal reteFuenteAmount, BigDecimal reteIvaAmount,
                              BigDecimal reteIcaAmount) {
        validate(companyId, documentType, issueDate, issueTime, dianStatus, issuer, customer,
                lineExtensionAmount, taxExclusiveAmount, taxInclusiveAmount, payableAmount, paymentForm, lines);
        validateNoteConsistency(documentType, reference, noteReasonCode);
        this.id = id;
        this.companyId = companyId;
        this.openAccountId = openAccountId;
        this.documentType = documentType;
        this.prefix = prefix;
        this.consecutive = consecutive;
        this.resolutionNumber = resolutionNumber;
        this.issueDate = issueDate;
        this.issueTime = issueTime;
        this.cufe = cufe;
        this.cude = cude;
        this.uuid = uuid;
        this.qrData = qrData;
        this.qrUrl = qrUrl;
        this.xmlSigned = xmlSigned;
        this.pdfRepresentation = pdfRepresentation;
        this.dianStatus = dianStatus;
        this.dianValidationDate = dianValidationDate;
        this.issuer = issuer;
        this.customer = customer;
        this.lineExtensionAmount = lineExtensionAmount;
        this.taxExclusiveAmount = taxExclusiveAmount;
        this.taxInclusiveAmount = taxInclusiveAmount;
        this.payableAmount = payableAmount;
        this.paymentForm = paymentForm;
        this.paymentDueDate = paymentDueDate;
        this.lines = List.copyOf(lines);
        this.payments = payments == null ? List.of() : List.copyOf(payments);
        this.reference = reference;
        this.noteReasonCode = noteReasonCode;
        this.noteReasonText = noteReasonText;
        this.reversed = reversed;
        this.reteFuenteAmount = reteFuenteAmount == null ? BigDecimal.ZERO : reteFuenteAmount;
        this.reteIvaAmount = reteIvaAmount == null ? BigDecimal.ZERO : reteIvaAmount;
        this.reteIcaAmount = reteIcaAmount == null ? BigDecimal.ZERO : reteIcaAmount;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    /**
     * Construye un documento PENDIENTE a partir de los datos ya congelados de una cuenta cerrada.
     * Estampa fecha/hora Colombia (-05:00) y totaliza desde las lineas. Sin numero ni sellos DIAN.
     */
    public static ElectronicDocument createPending(Long companyId, Long openAccountId,
                                                   ElectronicDocumentType documentType, IssuerSnapshot issuer,
                                                   CustomerSnapshot customer, List<ElectronicDocumentLine> lines,
                                                   List<ElectronicDocumentPayment> payments,
                                                   PaymentForm paymentForm, LocalDate paymentDueDate,
                                                   boolean withholdingAgent, BigDecimal reteFuenteRate,
                                                   BigDecimal reteIvaRate, BigDecimal reteIcaRate) {
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("a document requires at least one line");
        ZonedDateTime now = ZonedDateTime.now(COLOMBIA);
        String issueTime = now.toLocalTime().format(TIME_FORMAT) + COLOMBIA_OFFSET;
        BigDecimal base = sum(lines, ElectronicDocumentLine::getLineExtensionAmount);
        BigDecimal totalWithTax = sum(lines, ElectronicDocumentLine::getTotalAmount);
        BigDecimal iva = totalWithTax.subtract(base);
        // F6: si el adquiriente es agente retenedor, calcula las retenciones con las tarifas del emisor.
        WithholdingAmounts wh = WithholdingCalculator.compute(withholdingAgent, base, iva,
                reteFuenteRate, reteIvaRate, reteIcaRate);
        return new ElectronicDocument(null, companyId, openAccountId, documentType,
                null, null, null, now.toLocalDate(), issueTime,
                null, null, null, null, null, null, null, DianStatus.PENDIENTE, null,
                issuer, customer, base, base, totalWithTax, totalWithTax,
                paymentForm, paymentDueDate, lines, payments, LocalDateTime.now(), true,
                null, null, null, false, wh.reteFuente(), wh.reteIva(), wh.reteIca());
    }

    /**
     * Construye una NOTA CREDITO PENDIENTE que corrige (anula, total) una factura VALIDADA. Clona el
     * contenido fiscal del original (lineas, snapshots, totales) y fija la referencia + concepto DIAN.
     * La nota usa CUDE propio y consecutivo de su resolucion (los estampa la validacion DIAN, F3).
     */
    public static ElectronicDocument createCreditNote(ElectronicDocument original,
                                                      String reasonCode, String reasonText) {
        return createNote(original, ElectronicDocumentType.NOTA_CREDITO, reasonCode, reasonText);
    }

    /** Construye una NOTA DEBITO PENDIENTE (aumentos) que referencia una factura VALIDADA. */
    public static ElectronicDocument createDebitNote(ElectronicDocument original,
                                                     String reasonCode, String reasonText) {
        return createNote(original, ElectronicDocumentType.NOTA_DEBITO, reasonCode, reasonText);
    }

    private static ElectronicDocument createNote(ElectronicDocument original, ElectronicDocumentType type,
                                                 String reasonCode, String reasonText) {
        if (original == null) throw new IllegalArgumentException("original document is required");
        if (original.cufe == null || original.cufe.isBlank())
            throw new IllegalArgumentException("original document has no CUFE to reference");
        DocumentReference ref = new DocumentReference(original.cufe, original.prefix,
                original.consecutive, original.issueDate);
        List<ElectronicDocumentLine> clonedLines = original.lines.stream()
                .map(l -> new ElectronicDocumentLine(null, l.getLineNumber(), l.getDescription(), l.getQuantity(),
                        l.getUnitMeasureCode(), l.getUnitPrice(), l.getLineExtensionAmount(), l.getTaxCategory(),
                        l.getTaxScheme(), l.getTaxRate(), l.getTaxAmount(), l.getTotalAmount()))
                .toList();
        List<ElectronicDocumentPayment> clonedPayments = original.payments.stream()
                .map(p -> new ElectronicDocumentPayment(null, p.getPaymentMeans(), p.getAmount()))
                .toList();
        ZonedDateTime now = ZonedDateTime.now(COLOMBIA);
        String issueTime = now.toLocalTime().format(TIME_FORMAT) + COLOMBIA_OFFSET;
        return new ElectronicDocument(null, original.companyId, original.openAccountId, type,
                null, null, null, now.toLocalDate(), issueTime,
                null, null, null, null, null, null, null, DianStatus.PENDIENTE, null,
                original.issuer, original.customer, original.lineExtensionAmount, original.taxExclusiveAmount,
                original.taxInclusiveAmount, original.payableAmount, original.paymentForm, original.paymentDueDate,
                clonedLines, clonedPayments, LocalDateTime.now(), true,
                ref, reasonCode, reasonText, false,
                original.reteFuenteAmount, original.reteIvaAmount, original.reteIcaAmount);
    }

    private static BigDecimal sum(List<ElectronicDocumentLine> lines,
                                  java.util.function.Function<ElectronicDocumentLine, BigDecimal> field) {
        return lines.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static void validate(Long companyId, ElectronicDocumentType documentType, LocalDate issueDate,
                                 String issueTime, DianStatus dianStatus, IssuerSnapshot issuer,
                                 CustomerSnapshot customer, BigDecimal lineExtensionAmount,
                                 BigDecimal taxExclusiveAmount, BigDecimal taxInclusiveAmount,
                                 BigDecimal payableAmount, PaymentForm paymentForm,
                                 List<ElectronicDocumentLine> lines) {
        if (companyId == null) throw new IllegalArgumentException("companyId is required");
        if (documentType == null) throw new IllegalArgumentException("documentType is required");
        if (issueDate == null) throw new IllegalArgumentException("issueDate is required");
        if (issueTime == null || issueTime.isBlank()) throw new IllegalArgumentException("issueTime is required");
        if (dianStatus == null) throw new IllegalArgumentException("dianStatus is required");
        if (issuer == null) throw new IllegalArgumentException("issuer snapshot is required");
        if (customer == null) throw new IllegalArgumentException("customer snapshot is required");
        if (lineExtensionAmount == null) throw new IllegalArgumentException("lineExtensionAmount is required");
        if (taxExclusiveAmount == null) throw new IllegalArgumentException("taxExclusiveAmount is required");
        if (taxInclusiveAmount == null) throw new IllegalArgumentException("taxInclusiveAmount is required");
        if (payableAmount == null) throw new IllegalArgumentException("payableAmount is required");
        if (paymentForm == null) throw new IllegalArgumentException("paymentForm is required");
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("a document requires at least one line");
    }

    /** Una nota (credito/debito) exige referencia + concepto; una factura no puede llevarlos. */
    private static void validateNoteConsistency(ElectronicDocumentType documentType,
                                                DocumentReference reference, String noteReasonCode) {
        boolean isNote = documentType == ElectronicDocumentType.NOTA_CREDITO
                || documentType == ElectronicDocumentType.NOTA_DEBITO;
        if (isNote) {
            if (reference == null)
                throw new IllegalArgumentException("a credit/debit note requires a document reference");
            if (noteReasonCode == null || noteReasonCode.isBlank())
                throw new IllegalArgumentException("a credit/debit note requires a reason code");
        } else if (reference != null) {
            throw new IllegalArgumentException("an invoice cannot reference another document");
        }
    }

    public boolean isNote() {
        return documentType == ElectronicDocumentType.NOTA_CREDITO
                || documentType == ElectronicDocumentType.NOTA_DEBITO;
    }

    /**
     * Código DIAN del medio de pago predominante (el de mayor monto). Si el documento no tiene pagos
     * registrados, asume EFECTIVO (10). Lo usan los adaptadores de proveedor para `payment_method_code`.
     */
    public String primaryPaymentMeansCode() {
        return payments.stream()
                .max(Comparator.comparing(ElectronicDocumentPayment::getAmount))
                .map(p -> p.getPaymentMeans().dianCode())
                .orElse(PaymentMeans.EFECTIVO.dianCode());
    }

    /**
     * Asigna la numeración fiscal (resolución DIAN + prefijo + consecutivo) al emitir, antes de transmitir.
     * Tomada de la {@code NumberingResolution} activa de la empresa. Solo sobre un documento PENDIENTE y
     * aún sin numerar (idempotencia/seguridad fiscal: el consecutivo no se reasigna).
     */
    public void assignNumber(String resolutionNumber, String prefix, Long consecutive) {
        if (dianStatus != DianStatus.PENDIENTE)
            throw new IllegalStateException("solo se puede numerar un documento PENDIENTE");
        if (this.consecutive != null)
            throw new IllegalStateException("el documento ya tiene consecutivo asignado");
        if (resolutionNumber == null || resolutionNumber.isBlank())
            throw new IllegalArgumentException("resolutionNumber is required");
        if (consecutive == null) throw new IllegalArgumentException("consecutive is required");
        this.resolutionNumber = resolutionNumber;
        this.prefix = prefix;
        this.consecutive = consecutive;
    }

    /**
     * Sella el documento como VALIDADO por la DIAN: número fiscal + sellos del proveedor. Forward-only:
     * solo desde PENDIENTE/CONTINGENCIA. No toca el contenido fiscal.
     */
    public void markValidated(String prefix, Long consecutive, String cufe, String cude, String uuid,
                              String xmlSigned, String qrData, String qrUrl, String pdfRepresentation,
                              LocalDateTime validationDate) {
        ensureNotTerminal();
        // El numero fiscal lo asignamos nosotros al emitir (assignNumber); no lo sobreescribas si el
        // proveedor no lo devuelve (MATIAS no lo regresa en /invoice). Solo lo aplica si viene poblado.
        if (prefix != null) this.prefix = prefix;
        if (consecutive != null) this.consecutive = consecutive;
        this.cufe = cufe;
        this.cude = cude;
        this.uuid = uuid;
        this.xmlSigned = xmlSigned;
        this.qrData = qrData;
        this.qrUrl = qrUrl;
        this.pdfRepresentation = pdfRepresentation;
        this.dianValidationDate = validationDate;
        this.dianStatus = DianStatus.VALIDADO;
    }

    /** La DIAN rechazó el documento. El motivo se registra en la bitácora de transmisión. */
    public void markRejected() {
        ensureNotTerminal();
        this.dianStatus = DianStatus.RECHAZADO;
    }

    /** Proveedor/DIAN indisponible: queda en contingencia para retransmitir dentro del plazo. */
    public void markContingency() {
        ensureNotTerminal();
        this.dianStatus = DianStatus.CONTINGENCIA;
    }

    /** Adjunta la referencia (clave/URL S3) de la representación gráfica PDF ya generada. */
    public void attachRepresentation(String pdfRepresentation) {
        this.pdfRepresentation = pdfRepresentation;
    }

    /**
     * Marca esta factura como reversada por una nota credito ya VALIDADA. Idempotente. Es la marca
     * fiscal que subordina el void de cartera a la validacion DIAN de la nota (no es soft-delete).
     */
    public void markReversed() {
        this.reversed = true;
    }

    private void ensureNotTerminal() {
        if (dianStatus == DianStatus.VALIDADO || dianStatus == DianStatus.RECHAZADO) {
            throw new IllegalStateException(
                    "El documento ya está en estado terminal (" + dianStatus
                            + ") y no puede transicionar; corrige por nota crédito/débito.");
        }
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public Long getOpenAccountId() { return openAccountId; }
    public ElectronicDocumentType getDocumentType() { return documentType; }
    public String getPrefix() { return prefix; }
    public Long getConsecutive() { return consecutive; }
    public String getResolutionNumber() { return resolutionNumber; }
    public LocalDate getIssueDate() { return issueDate; }
    public String getIssueTime() { return issueTime; }
    public String getCufe() { return cufe; }
    public String getCude() { return cude; }
    public String getUuid() { return uuid; }
    public String getQrData() { return qrData; }
    public String getQrUrl() { return qrUrl; }
    public String getXmlSigned() { return xmlSigned; }
    public String getPdfRepresentation() { return pdfRepresentation; }
    public DianStatus getDianStatus() { return dianStatus; }
    public LocalDateTime getDianValidationDate() { return dianValidationDate; }
    public IssuerSnapshot getIssuer() { return issuer; }
    public CustomerSnapshot getCustomer() { return customer; }
    public BigDecimal getLineExtensionAmount() { return lineExtensionAmount; }
    public BigDecimal getTaxExclusiveAmount() { return taxExclusiveAmount; }
    public BigDecimal getTaxInclusiveAmount() { return taxInclusiveAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public BigDecimal getReteFuenteAmount() { return reteFuenteAmount; }
    public BigDecimal getReteIvaAmount() { return reteIvaAmount; }
    public BigDecimal getReteIcaAmount() { return reteIcaAmount; }
    /** Neto a pagar tras restar las retenciones del adquiriente (total − reteFuente − reteIva − reteIca). */
    public BigDecimal getNetPayableAmount() {
        return payableAmount.subtract(reteFuenteAmount).subtract(reteIvaAmount).subtract(reteIcaAmount);
    }
    public PaymentForm getPaymentForm() { return paymentForm; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public List<ElectronicDocumentLine> getLines() { return lines; }
    public List<ElectronicDocumentPayment> getPayments() { return payments; }
    public DocumentReference getReference() { return reference; }
    public String getNoteReasonCode() { return noteReasonCode; }
    public String getNoteReasonText() { return noteReasonText; }
    public boolean isReversed() { return reversed; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
}
