package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Documento electronico (cabecera). INMUTABLE: sin @SQLDelete/@SQLRestriction (nunca se soft-deletea
 * ni edita; solo se corrige por nota credito/debito). Snapshots de emisor/adquiriente como columnas.
 */
@Entity
@Table(name = "electronic_documents")
public class ElectronicDocumentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "open_account_id")
    private Long openAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private ElectronicDocumentType documentType;

    @Column(name = "prefix", length = 10)
    private String prefix;

    @Column(name = "consecutive")
    private Long consecutive;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "issue_time", nullable = false, length = 20)
    private String issueTime;

    @Column(name = "cufe", length = 100)
    private String cufe;

    @Column(name = "cude", length = 100)
    private String cude;

    @Column(name = "uuid", length = 64)
    private String uuid;

    @Column(name = "qr_data", length = 2000)
    private String qrData;

    @Column(name = "qr_url", length = 512)
    private String qrUrl;

    @Column(name = "xml_signed", length = 2000)
    private String xmlSigned;

    @Column(name = "pdf_representation", length = 512)
    private String pdfRepresentation;

    @Enumerated(EnumType.STRING)
    @Column(name = "dian_status", nullable = false, length = 20)
    private DianStatus dianStatus;

    @Column(name = "dian_validation_date")
    private LocalDateTime dianValidationDate;

    @Column(name = "issuer_document_type", length = 30)
    private String issuerDocumentType;
    @Column(name = "issuer_document_id", length = 20)
    private String issuerDocumentId;
    @Column(name = "issuer_verification_digit", length = 1)
    private String issuerVerificationDigit;
    @Column(name = "issuer_legal_name", length = 255)
    private String issuerLegalName;
    @Column(name = "issuer_tax_regime", length = 30)
    private String issuerTaxRegime;
    @Column(name = "issuer_email", length = 255)
    private String issuerEmail;

    @Column(name = "customer_document_type", length = 30)
    private String customerDocumentType;
    @Column(name = "customer_document_id", length = 50)
    private String customerDocumentId;
    @Column(name = "customer_verification_digit", length = 1)
    private String customerVerificationDigit;
    @Column(name = "customer_person_type", length = 20)
    private String customerPersonType;
    @Column(name = "customer_legal_name", length = 255)
    private String customerLegalName;
    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "line_extension_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineExtensionAmount;
    @Column(name = "tax_exclusive_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxExclusiveAmount;
    @Column(name = "tax_inclusive_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxInclusiveAmount;
    @Column(name = "payable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal payableAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_form", nullable = false, length = 20)
    private PaymentForm paymentForm;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    // Set (no List) para permitir fetch conjunto via @EntityGraph sin MultipleBagFetchException.
    // El orden de lineas se reconstruye por lineNumber en el mapper.
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ElectronicDocumentLineJpaEntity> lines = new LinkedHashSet<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ElectronicDocumentPaymentJpaEntity> payments = new LinkedHashSet<>();

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected ElectronicDocumentJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public Long getOpenAccountId() { return openAccountId; }
    public void setOpenAccountId(Long openAccountId) { this.openAccountId = openAccountId; }
    public ElectronicDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(ElectronicDocumentType documentType) { this.documentType = documentType; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public Long getConsecutive() { return consecutive; }
    public void setConsecutive(Long consecutive) { this.consecutive = consecutive; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public String getIssueTime() { return issueTime; }
    public void setIssueTime(String issueTime) { this.issueTime = issueTime; }
    public String getCufe() { return cufe; }
    public void setCufe(String cufe) { this.cufe = cufe; }
    public String getCude() { return cude; }
    public void setCude(String cude) { this.cude = cude; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getQrData() { return qrData; }
    public void setQrData(String qrData) { this.qrData = qrData; }
    public String getQrUrl() { return qrUrl; }
    public void setQrUrl(String qrUrl) { this.qrUrl = qrUrl; }
    public String getXmlSigned() { return xmlSigned; }
    public void setXmlSigned(String xmlSigned) { this.xmlSigned = xmlSigned; }
    public String getPdfRepresentation() { return pdfRepresentation; }
    public void setPdfRepresentation(String pdfRepresentation) { this.pdfRepresentation = pdfRepresentation; }
    public DianStatus getDianStatus() { return dianStatus; }
    public void setDianStatus(DianStatus dianStatus) { this.dianStatus = dianStatus; }
    public LocalDateTime getDianValidationDate() { return dianValidationDate; }
    public void setDianValidationDate(LocalDateTime dianValidationDate) { this.dianValidationDate = dianValidationDate; }
    public String getIssuerDocumentType() { return issuerDocumentType; }
    public void setIssuerDocumentType(String v) { this.issuerDocumentType = v; }
    public String getIssuerDocumentId() { return issuerDocumentId; }
    public void setIssuerDocumentId(String v) { this.issuerDocumentId = v; }
    public String getIssuerVerificationDigit() { return issuerVerificationDigit; }
    public void setIssuerVerificationDigit(String v) { this.issuerVerificationDigit = v; }
    public String getIssuerLegalName() { return issuerLegalName; }
    public void setIssuerLegalName(String v) { this.issuerLegalName = v; }
    public String getIssuerTaxRegime() { return issuerTaxRegime; }
    public void setIssuerTaxRegime(String v) { this.issuerTaxRegime = v; }
    public String getIssuerEmail() { return issuerEmail; }
    public void setIssuerEmail(String v) { this.issuerEmail = v; }
    public String getCustomerDocumentType() { return customerDocumentType; }
    public void setCustomerDocumentType(String v) { this.customerDocumentType = v; }
    public String getCustomerDocumentId() { return customerDocumentId; }
    public void setCustomerDocumentId(String v) { this.customerDocumentId = v; }
    public String getCustomerVerificationDigit() { return customerVerificationDigit; }
    public void setCustomerVerificationDigit(String v) { this.customerVerificationDigit = v; }
    public String getCustomerPersonType() { return customerPersonType; }
    public void setCustomerPersonType(String v) { this.customerPersonType = v; }
    public String getCustomerLegalName() { return customerLegalName; }
    public void setCustomerLegalName(String v) { this.customerLegalName = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
    public BigDecimal getLineExtensionAmount() { return lineExtensionAmount; }
    public void setLineExtensionAmount(BigDecimal v) { this.lineExtensionAmount = v; }
    public BigDecimal getTaxExclusiveAmount() { return taxExclusiveAmount; }
    public void setTaxExclusiveAmount(BigDecimal v) { this.taxExclusiveAmount = v; }
    public BigDecimal getTaxInclusiveAmount() { return taxInclusiveAmount; }
    public void setTaxInclusiveAmount(BigDecimal v) { this.taxInclusiveAmount = v; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal v) { this.payableAmount = v; }
    public PaymentForm getPaymentForm() { return paymentForm; }
    public void setPaymentForm(PaymentForm paymentForm) { this.paymentForm = paymentForm; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }
    public Set<ElectronicDocumentLineJpaEntity> getLines() { return lines; }
    public void setLines(Set<ElectronicDocumentLineJpaEntity> lines) { this.lines = lines; }
    public Set<ElectronicDocumentPaymentJpaEntity> getPayments() { return payments; }
    public void setPayments(Set<ElectronicDocumentPaymentJpaEntity> payments) { this.payments = payments; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
