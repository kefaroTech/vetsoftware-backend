package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Cabecera de la cotizacion con su detalle.
 *
 * <p>
 * <b>company es NULABLE</b> y esa es la particularidad del bloque: se cotiza a
 * un prospecto que todavia no es empresa. Esta cabecera es la frontera de
 * tenant de {@code quote_lines}, que por eso no lleva {@code company_id} -una
 * FK compuesta con una columna nula del padre nunca se comprobaria-.
 *
 * <p>
 * <b>priceListId es una columna plana, no un {@code @ManyToOne}</b>, y tampoco
 * lo es {@code catalogItemId} en la linea. La FK existe en la base para poder
 * navegar; modelar la asociacion en Java seria abrirle la puerta a que alguien
 * releyese el precio o el nombre del catalogo al pintar una cotizacion vieja,
 * que es precisamente lo que este documento existe para impedir.
 *
 * <p>
 * <b>La baja logica NO pasa por {@code deleteById()}.</b> Va por el UPDATE
 * nativo de {@link QuoteJpaRepository}: el borrado en cascada del detalle lo
 * emite Hibernate ANTES del {@code @SQLDelete} de la raiz y no hay forma de
 * interceptarlo, asi que un {@code deleteById()} pausaria la cabecera y
 * destruiria las lineas congeladas, que es justo la prueba que hay que
 * conservar. El {@code AND version = ?} del {@code @SQLDelete} es obligatorio
 * porque la entidad lleva {@code @Version}: Hibernate liga dos parametros.
 */
@Entity
@Table(name = "quotes")
@SQLDelete(sql = "UPDATE quotes SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class QuoteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quote_number", nullable = false, length = 30)
    private String quoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyJpaEntity company;

    @Column(name = "prospect_name", length = 150)
    private String prospectName;

    @Column(name = "prospect_email", length = 120)
    private String prospectEmail;

    @Column(name = "prospect_document", length = 50)
    private String prospectDocument;

    @Column(name = "prospect_phone", length = 30)
    private String prospectPhone;

    @Column(name = "price_list_id", nullable = false)
    private Long priceListId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuoteStatus status;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Column(name = "trial_days", nullable = false)
    private int trialDays;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "accepted_by_email", length = 120)
    private String acceptedByEmail;

    @Column(name = "accepted_ip", length = 45)
    private String acceptedIp;

    @Column(name = "client_request_id", nullable = false, length = 64)
    private String clientRequestId;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @JoinColumn(name = "quote_id", nullable = false)
    private Set<QuoteLineJpaEntity> lines = new LinkedHashSet<>();

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected QuoteJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuoteNumber() {
        return quoteNumber;
    }

    public void setQuoteNumber(String quoteNumber) {
        this.quoteNumber = quoteNumber;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public String getProspectName() {
        return prospectName;
    }

    public void setProspectName(String prospectName) {
        this.prospectName = prospectName;
    }

    public String getProspectEmail() {
        return prospectEmail;
    }

    public void setProspectEmail(String prospectEmail) {
        this.prospectEmail = prospectEmail;
    }

    public String getProspectDocument() {
        return prospectDocument;
    }

    public void setProspectDocument(String prospectDocument) {
        this.prospectDocument = prospectDocument;
    }

    public String getProspectPhone() {
        return prospectPhone;
    }

    public void setProspectPhone(String prospectPhone) {
        this.prospectPhone = prospectPhone;
    }

    public Long getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(Long priceListId) {
        this.priceListId = priceListId;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public void setStatus(QuoteStatus status) {
        this.status = status;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(int trialDays) {
        this.trialDays = trialDays;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getAcceptedByEmail() {
        return acceptedByEmail;
    }

    public void setAcceptedByEmail(String acceptedByEmail) {
        this.acceptedByEmail = acceptedByEmail;
    }

    public String getAcceptedIp() {
        return acceptedIp;
    }

    public void setAcceptedIp(String acceptedIp) {
        this.acceptedIp = acceptedIp;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }

    public Set<QuoteLineJpaEntity> getLines() {
        return lines;
    }

    public void setLines(Set<QuoteLineJpaEntity> lines) {
        this.lines = lines;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
