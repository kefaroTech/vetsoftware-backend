package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Perfil fiscal de la empresa con sus responsabilidades DIAN.
 *
 * <p>
 * <b>Ojo con el borrado en cascada.</b> El {@code @SQLDelete} solo sustituye el
 * DELETE de la raíz: el borrado en cascada de las responsabilidades lo emite
 * Hibernate <i>antes</i>, y no hay forma de interceptarlo. El
 * {@code orphanRemoval} de {@link #responsibilities} es imprescindible para el
 * camino de edición (quitar una responsabilidad debe borrar su fila) y por sí
 * solo ya propaga el borrado al eliminar el padre, así que llamar a
 * {@code deleteById()} sobre esta entidad destruiría el detalle fiscal.
 */
@Entity
@Table(name = "company_tax_profiles")
@SQLDelete(sql = "UPDATE company_tax_profiles SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class CompanyTaxProfileJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private CompanyJpaEntity company;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_document_type", nullable = false, length = 30)
    private CompanyDocumentType companyDocumentType;

    @Column(name = "company_document_id", nullable = false, length = 20)
    private String companyDocumentId;

    @Column(name = "company_document_verification_digit", length = 1)
    private String companyDocumentVerificationDigit;

    @Column(name = "legal_name", nullable = false, length = 255)
    private String legalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 30)
    private TaxRegime taxRegime;

    @Column(name = "fiscal_email", nullable = false, length = 255)
    private String fiscalEmail;

    @Column(name = "commercial_name", length = 150)
    private String commercialName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "economic_activity_id")
    private EconomicActivityJpaEntity economicActivity;

    @OneToMany(mappedBy = "companyTaxProfile", cascade = {CascadeType.PERSIST,
            CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompanyTaxProfileResponsibilityJpaEntity> responsibilities = new ArrayList<>();

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected CompanyTaxProfileJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public CompanyDocumentType getCompanyDocumentType() {
        return companyDocumentType;
    }

    public void setCompanyDocumentType(CompanyDocumentType companyDocumentType) {
        this.companyDocumentType = companyDocumentType;
    }

    public String getCompanyDocumentId() {
        return companyDocumentId;
    }

    public void setCompanyDocumentId(String companyDocumentId) {
        this.companyDocumentId = companyDocumentId;
    }

    public String getCompanyDocumentVerificationDigit() {
        return companyDocumentVerificationDigit;
    }

    public void setCompanyDocumentVerificationDigit(String companyDocumentVerificationDigit) {
        this.companyDocumentVerificationDigit = companyDocumentVerificationDigit;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public void setTaxRegime(TaxRegime taxRegime) {
        this.taxRegime = taxRegime;
    }

    public String getFiscalEmail() {
        return fiscalEmail;
    }

    public void setFiscalEmail(String fiscalEmail) {
        this.fiscalEmail = fiscalEmail;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    public EconomicActivityJpaEntity getEconomicActivity() {
        return economicActivity;
    }

    public void setEconomicActivity(EconomicActivityJpaEntity economicActivity) {
        this.economicActivity = economicActivity;
    }

    public List<CompanyTaxProfileResponsibilityJpaEntity> getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(
            List<CompanyTaxProfileResponsibilityJpaEntity> responsibilities) {
        this.responsibilities = responsibilities;
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
