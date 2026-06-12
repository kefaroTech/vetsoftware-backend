package com.vetsoftware.app.companytaxprofile.domain;

import java.time.LocalDateTime;
import java.util.List;

public class CompanyTaxProfile {
    private static final String EMAIL_REGEX = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private Long id;
    private CompanyRef company;
    private CompanyDocumentType companyDocumentType;
    private String companyDocumentId;
    private String companyDocumentVerificationDigit;
    private String legalName;
    private TaxRegime taxRegime;
    private String fiscalEmail;
    private String commercialName;
    private EconomicActivityRef economicActivity;
    private List<CompanyTaxProfileResponsibility> responsibilities;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public CompanyTaxProfile(Long id,
                             CompanyRef company,
                             CompanyDocumentType companyDocumentType,
                             String companyDocumentId,
                             String companyDocumentVerificationDigit,
                             String legalName,
                             TaxRegime taxRegime,
                             String fiscalEmail,
                             String commercialName,
                             EconomicActivityRef economicActivity,
                             List<CompanyTaxProfileResponsibility> responsibilities,
                             LocalDateTime createdDate,
                             boolean enabled) {
        validate(company, companyDocumentType, companyDocumentId, companyDocumentVerificationDigit,
                legalName, taxRegime, fiscalEmail, responsibilities);
        this.id = id;
        this.company = company;
        this.companyDocumentType = companyDocumentType;
        this.companyDocumentId = companyDocumentId;
        this.companyDocumentVerificationDigit =
                (companyDocumentVerificationDigit == null || companyDocumentVerificationDigit.isBlank())
                        ? null : companyDocumentVerificationDigit;
        this.legalName = legalName;
        this.taxRegime = taxRegime;
        this.fiscalEmail = fiscalEmail;
        this.commercialName = commercialName;
        this.economicActivity = economicActivity;
        this.responsibilities = List.copyOf(responsibilities);
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static CompanyTaxProfile create(CompanyRef company,
                                           CompanyDocumentType companyDocumentType,
                                           String companyDocumentId,
                                           String companyDocumentVerificationDigit,
                                           String legalName,
                                           TaxRegime taxRegime,
                                           String fiscalEmail,
                                           String commercialName,
                                           EconomicActivityRef economicActivity,
                                           List<CompanyTaxProfileResponsibility> responsibilities) {
        return new CompanyTaxProfile(null, company, companyDocumentType, companyDocumentId,
                companyDocumentVerificationDigit, legalName, taxRegime, fiscalEmail, commercialName,
                economicActivity, responsibilities, LocalDateTime.now(), true);
    }

    public void update(CompanyDocumentType companyDocumentType,
                       String companyDocumentId,
                       String companyDocumentVerificationDigit,
                       String legalName,
                       TaxRegime taxRegime,
                       String fiscalEmail,
                       String commercialName,
                       EconomicActivityRef economicActivity,
                       List<CompanyTaxProfileResponsibility> responsibilities) {
        validate(this.company, companyDocumentType, companyDocumentId, companyDocumentVerificationDigit,
                legalName, taxRegime, fiscalEmail, responsibilities);
        this.companyDocumentType = companyDocumentType;
        this.companyDocumentId = companyDocumentId;
        this.companyDocumentVerificationDigit =
                (companyDocumentVerificationDigit == null || companyDocumentVerificationDigit.isBlank())
                        ? null : companyDocumentVerificationDigit;
        this.legalName = legalName;
        this.taxRegime = taxRegime;
        this.fiscalEmail = fiscalEmail;
        this.commercialName = commercialName;
        this.economicActivity = economicActivity;
        this.responsibilities = List.copyOf(responsibilities);
    }

    private static void validate(CompanyRef company,
                                 CompanyDocumentType companyDocumentType,
                                 String companyDocumentId,
                                 String companyDocumentVerificationDigit,
                                 String legalName,
                                 TaxRegime taxRegime,
                                 String fiscalEmail,
                                 List<CompanyTaxProfileResponsibility> responsibilities) {
        if (company == null) throw new IllegalArgumentException("company is required");
        if (companyDocumentType == null) throw new IllegalArgumentException("companyDocumentType is required");
        if (taxRegime == null) throw new IllegalArgumentException("taxRegime is required");

        if (companyDocumentId == null || companyDocumentId.isBlank())
            throw new IllegalArgumentException("companyDocumentId is required");
        if (companyDocumentId.length() > 20)
            throw new IllegalArgumentException("companyDocumentId must be 20 chars or less");

        if (legalName == null || legalName.isBlank())
            throw new IllegalArgumentException("legalName is required");
        if (legalName.length() > 255)
            throw new IllegalArgumentException("legalName must be 255 chars or less");

        if (fiscalEmail == null || fiscalEmail.isBlank())
            throw new IllegalArgumentException("fiscalEmail is required");
        if (fiscalEmail.length() > 255)
            throw new IllegalArgumentException("fiscalEmail must be 255 chars or less");
        if (!fiscalEmail.matches(EMAIL_REGEX))
            throw new IllegalArgumentException("fiscalEmail must be a valid email");

        if (responsibilities == null || responsibilities.isEmpty())
            throw new IllegalArgumentException("at least one responsibility is required");

        boolean hasVerificationDigit =
                companyDocumentVerificationDigit != null && !companyDocumentVerificationDigit.isBlank();
        if (companyDocumentType == CompanyDocumentType.NIT) {
            if (!hasVerificationDigit)
                throw new IllegalArgumentException("companyDocumentVerificationDigit is required when document type is NIT");
            if (!companyDocumentVerificationDigit.matches("[0-9]"))
                throw new IllegalArgumentException("companyDocumentVerificationDigit must be a single digit 0-9");
        } else if (hasVerificationDigit) {
            throw new IllegalArgumentException("companyDocumentVerificationDigit is only allowed when document type is NIT");
        }
    }

    public Long getId() { return id; }
    public CompanyRef getCompany() { return company; }
    public CompanyDocumentType getCompanyDocumentType() { return companyDocumentType; }
    public String getCompanyDocumentId() { return companyDocumentId; }
    public String getCompanyDocumentVerificationDigit() { return companyDocumentVerificationDigit; }
    public String getLegalName() { return legalName; }
    public TaxRegime getTaxRegime() { return taxRegime; }
    public String getFiscalEmail() { return fiscalEmail; }
    public String getCommercialName() { return commercialName; }
    public EconomicActivityRef getEconomicActivity() { return economicActivity; }
    public List<CompanyTaxProfileResponsibility> getResponsibilities() { return responsibilities; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
