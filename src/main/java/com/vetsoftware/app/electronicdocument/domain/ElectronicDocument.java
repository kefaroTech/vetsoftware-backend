package com.vetsoftware.app.electronicdocument.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    private final PaymentForm paymentForm;
    private final LocalDate paymentDueDate;

    private final List<ElectronicDocumentLine> lines;
    private final List<ElectronicDocumentPayment> payments;

    private final LocalDateTime createdDate;
    private final boolean enabled;

    public ElectronicDocument(Long id, Long companyId, Long openAccountId, ElectronicDocumentType documentType,
                              String prefix, Long consecutive, LocalDate issueDate, String issueTime,
                              String cufe, String cude, String uuid, String qrData, String qrUrl,
                              String xmlSigned, String pdfRepresentation, DianStatus dianStatus,
                              LocalDateTime dianValidationDate, IssuerSnapshot issuer, CustomerSnapshot customer,
                              BigDecimal lineExtensionAmount, BigDecimal taxExclusiveAmount,
                              BigDecimal taxInclusiveAmount, BigDecimal payableAmount, PaymentForm paymentForm,
                              LocalDate paymentDueDate, List<ElectronicDocumentLine> lines,
                              List<ElectronicDocumentPayment> payments, LocalDateTime createdDate, boolean enabled) {
        validate(companyId, documentType, issueDate, issueTime, dianStatus, issuer, customer,
                lineExtensionAmount, taxExclusiveAmount, taxInclusiveAmount, payableAmount, paymentForm, lines);
        this.id = id;
        this.companyId = companyId;
        this.openAccountId = openAccountId;
        this.documentType = documentType;
        this.prefix = prefix;
        this.consecutive = consecutive;
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
                                                   PaymentForm paymentForm, LocalDate paymentDueDate) {
        if (lines == null || lines.isEmpty())
            throw new IllegalArgumentException("a document requires at least one line");
        ZonedDateTime now = ZonedDateTime.now(COLOMBIA);
        String issueTime = now.toLocalTime().format(TIME_FORMAT) + COLOMBIA_OFFSET;
        BigDecimal base = sum(lines, ElectronicDocumentLine::getLineExtensionAmount);
        BigDecimal totalWithTax = sum(lines, ElectronicDocumentLine::getTotalAmount);
        return new ElectronicDocument(null, companyId, openAccountId, documentType,
                null, null, now.toLocalDate(), issueTime,
                null, null, null, null, null, null, null, DianStatus.PENDIENTE, null,
                issuer, customer, base, base, totalWithTax, totalWithTax,
                paymentForm, paymentDueDate, lines, payments, LocalDateTime.now(), true);
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

    /**
     * Sella el documento como VALIDADO por la DIAN: número fiscal + sellos del proveedor. Forward-only:
     * solo desde PENDIENTE/CONTINGENCIA. No toca el contenido fiscal.
     */
    public void markValidated(String prefix, Long consecutive, String cufe, String cude, String uuid,
                              String xmlSigned, String qrData, String qrUrl, String pdfRepresentation,
                              LocalDateTime validationDate) {
        ensureNotTerminal();
        this.prefix = prefix;
        this.consecutive = consecutive;
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
    public PaymentForm getPaymentForm() { return paymentForm; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public List<ElectronicDocumentLine> getLines() { return lines; }
    public List<ElectronicDocumentPayment> getPayments() { return payments; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
}
