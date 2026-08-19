package com.vetsoftware.app.owner.domain;

import java.time.LocalDateTime;

public class Owner {
    private Long id;
    private String name;
    private String email;
    private String document;
    private OwnerDocumentType documentType;
    private PersonType personType;
    private String verificationDigit;
    private String legalName;
    private String address;
    private String phone;
    private CityRef city;
    private CompanyRef company;
    // F6 - agente retenedor: cuando el adquiriente lo es, la factura calcula y
    // muestra sus
    // retenciones.
    private boolean withholdingAgent;
    // Régimen de IVA del adquiriente (responsable / no responsable): determina el
    // tax_regime_id ante
    // la DIAN.
    private TaxRegime taxRegime;
    // Responsabilidad fiscal del adquiriente (RUT): determina el tax_level_id
    // (TaxLevelCode) ante la
    // DIAN.
    private FiscalResponsibility fiscalResponsibility;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean enabled;

    public Owner(Long id, String name, String email, String document,
            OwnerDocumentType documentType, PersonType personType, String verificationDigit,
            String legalName, String address, String phone, CityRef city, CompanyRef company,
            boolean withholdingAgent, TaxRegime taxRegime,
            FiscalResponsibility fiscalResponsibility, LocalDateTime createdDate, Long version,
            boolean enabled) {
        validate(name, email, document, documentType, personType, verificationDigit, legalName,
                address, phone, city, company, taxRegime, fiscalResponsibility);
        this.id = id;
        this.name = name;
        this.email = email;
        this.document = document;
        this.documentType = documentType;
        this.personType = personType;
        this.verificationDigit = normalizeVerificationDigit(verificationDigit);
        this.legalName = legalName;
        this.address = address;
        this.phone = phone;
        this.city = city;
        this.company = company;
        this.withholdingAgent = withholdingAgent;
        this.taxRegime = taxRegime;
        this.fiscalResponsibility = fiscalResponsibility;
        this.createdDate = createdDate;
        this.version = version;
        this.enabled = enabled;
    }

    public static Owner create(String name, String email, String document,
            OwnerDocumentType documentType, PersonType personType, String verificationDigit,
            String legalName, String address, String phone, CityRef city, CompanyRef company,
            boolean withholdingAgent, TaxRegime taxRegime,
            FiscalResponsibility fiscalResponsibility) {
        return new Owner(null, name, email, document, documentType, personType, verificationDigit,
                legalName, address, phone, city, company, withholdingAgent, taxRegime,
                fiscalResponsibility, LocalDateTime.now(), null, true);
    }

    public void update(String name, String email, String document, OwnerDocumentType documentType,
            PersonType personType, String verificationDigit, String legalName, String address,
            String phone, CityRef city, CompanyRef company, boolean withholdingAgent,
            TaxRegime taxRegime, FiscalResponsibility fiscalResponsibility) {
        validate(name, email, document, documentType, personType, verificationDigit, legalName,
                address, phone, city, company, taxRegime, fiscalResponsibility);
        this.name = name;
        this.email = email;
        this.document = document;
        this.documentType = documentType;
        this.personType = personType;
        this.verificationDigit = normalizeVerificationDigit(verificationDigit);
        this.legalName = legalName;
        this.address = address;
        this.phone = phone;
        this.city = city;
        this.company = company;
        this.withholdingAgent = withholdingAgent;
        this.taxRegime = taxRegime;
        this.fiscalResponsibility = fiscalResponsibility;
    }

    private static void validate(String name, String email, String document,
            OwnerDocumentType documentType, PersonType personType, String verificationDigit,
            String legalName, String address, String phone, CityRef city, CompanyRef company,
            TaxRegime taxRegime, FiscalResponsibility fiscalResponsibility) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 150)
            throw new IllegalArgumentException("name must be 150 chars or less");
        if (document == null || document.isBlank())
            throw new IllegalArgumentException("document is required");
        if (document.length() > 50)
            throw new IllegalArgumentException("document must be 50 chars or less");
        if (email != null && email.length() > 150)
            throw new IllegalArgumentException("email must be 150 chars or less");
        if (address != null && address.length() > 255)
            throw new IllegalArgumentException("address must be 255 chars or less");
        if (phone != null && phone.length() > 30)
            throw new IllegalArgumentException("phone must be 30 chars or less");
        if (city == null)
            throw new IllegalArgumentException("city is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
        if (taxRegime == null)
            throw new IllegalArgumentException("taxRegime is required");
        if (fiscalResponsibility == null)
            throw new IllegalArgumentException("fiscalResponsibility is required");

        if (documentType == null)
            throw new IllegalArgumentException("documentType is required");
        if (personType == null)
            throw new IllegalArgumentException("personType is required");

        if (legalName != null && legalName.length() > 255)
            throw new IllegalArgumentException("legalName must be 255 chars or less");

        boolean hasVerificationDigit = verificationDigit != null && !verificationDigit.isBlank();
        if (documentType == OwnerDocumentType.NIT) {
            if (!hasVerificationDigit)
                throw new IllegalArgumentException(
                        "verificationDigit is required when document type is NIT");
            if (!verificationDigit.matches("[0-9]"))
                throw new IllegalArgumentException("verificationDigit must be a single digit 0-9");
        } else if (hasVerificationDigit) {
            throw new IllegalArgumentException(
                    "verificationDigit is only allowed when document type is NIT");
        }

        if (personType == PersonType.JURIDICA) {
            if (documentType != OwnerDocumentType.NIT)
                throw new IllegalArgumentException(
                        "a juridical person must have document type NIT");
            if (legalName == null || legalName.isBlank())
                throw new IllegalArgumentException("legalName is required for a juridical person");
        }
    }

    private static String normalizeVerificationDigit(String verificationDigit) {
        return (verificationDigit == null || verificationDigit.isBlank())
                ? null
                : verificationDigit;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getDocument() {
        return document;
    }

    public OwnerDocumentType getDocumentType() {
        return documentType;
    }

    public PersonType getPersonType() {
        return personType;
    }

    public String getVerificationDigit() {
        return verificationDigit;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public CityRef getCity() {
        return city;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public boolean isWithholdingAgent() {
        return withholdingAgent;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public FiscalResponsibility getFiscalResponsibility() {
        return fiscalResponsibility;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }
}
