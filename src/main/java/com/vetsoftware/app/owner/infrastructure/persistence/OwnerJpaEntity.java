package com.vetsoftware.app.owner.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "owners", uniqueConstraints = {
    @UniqueConstraint(name = "uq_owners_company_document", columnNames = {"company_id", "document"})
})
@SQLDelete(sql = "UPDATE owners SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class OwnerJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 50)
    private String document;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private OwnerDocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 20)
    private PersonType personType;

    @Column(name = "verification_digit", length = 1)
    private String verificationDigit;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    // F6 - adquiriente agente retenedor: cuando lo es, la factura calcula y muestra sus retenciones.
    @Column(name = "withholding_agent", nullable = false)
    private boolean withholdingAgent = false;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private CityJpaEntity city;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected OwnerJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public OwnerDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(OwnerDocumentType documentType) { this.documentType = documentType; }
    public PersonType getPersonType() { return personType; }
    public void setPersonType(PersonType personType) { this.personType = personType; }
    public String getVerificationDigit() { return verificationDigit; }
    public void setVerificationDigit(String verificationDigit) { this.verificationDigit = verificationDigit; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public boolean isWithholdingAgent() { return withholdingAgent; }
    public void setWithholdingAgent(boolean withholdingAgent) { this.withholdingAgent = withholdingAgent; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public CityJpaEntity getCity() { return city; }
    public void setCity(CityJpaEntity city) { this.city = city; }
    public CompanyJpaEntity getCompany() { return company; }
    public void setCompany(CompanyJpaEntity company) { this.company = company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
