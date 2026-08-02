package com.vetsoftware.app.supplier.domain;

import java.time.LocalDateTime;

/**
 * Proveedor = ficha de un tercero al que la empresa le compra bienes o
 * servicios (identidad, contacto, condiciones comerciales). CRUD
 * company-scoped: cada proveedor pertenece a una empresa y su nombre es único
 * entre los proveedores activos de esa empresa.
 */
public class Supplier {
    private Long id;
    private String name;
    private String taxId;
    private String contactName;
    private String phone;
    private String email;
    private String address;
    private Integer paymentTermsDays;
    private String notes;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Long updatedBy;
    private Long version;
    private boolean enabled;

    public Supplier(Long id, String name, String taxId, String contactName, String phone,
            String email, String address, Integer paymentTermsDays, String notes,
            CompanyRef company, LocalDateTime createdDate, LocalDateTime updatedDate,
            Long updatedBy, Long version, boolean enabled) {
        validate(name, taxId, contactName, phone, email, address, paymentTermsDays, notes, company);
        this.id = id;
        this.name = name;
        this.taxId = taxId;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.paymentTermsDays = paymentTermsDays;
        this.notes = notes;
        this.company = company;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
        this.updatedBy = updatedBy;
        this.version = version;
        this.enabled = enabled;
    }

    public static Supplier create(String name, String taxId, String contactName, String phone,
            String email, String address, Integer paymentTermsDays, String notes,
            CompanyRef company) {
        return new Supplier(null, name, taxId, contactName, phone, email, address, paymentTermsDays,
                notes, company, LocalDateTime.now(), null, null, null, true);
    }

    public void update(String name, String taxId, String contactName, String phone, String email,
            String address, Integer paymentTermsDays, String notes, CompanyRef company,
            Long updatedBy, Long expectedVersion) {
        validate(name, taxId, contactName, phone, email, address, paymentTermsDays, notes, company);
        this.name = name;
        this.taxId = taxId;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.paymentTermsDays = paymentTermsDays;
        this.notes = notes;
        this.company = company;
        this.updatedDate = LocalDateTime.now();
        this.updatedBy = updatedBy;
        this.version = expectedVersion;
    }

    private static void validate(String name, String taxId, String contactName, String phone,
            String email, String address, Integer paymentTermsDays, String notes,
            CompanyRef company) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 150)
            throw new IllegalArgumentException("name must be 150 chars or less");
        if (taxId != null && taxId.length() > 30)
            throw new IllegalArgumentException("taxId must be 30 chars or less");
        if (contactName != null && contactName.length() > 100)
            throw new IllegalArgumentException("contactName must be 100 chars or less");
        if (phone != null && phone.length() > 30)
            throw new IllegalArgumentException("phone must be 30 chars or less");
        if (email != null && email.length() > 150)
            throw new IllegalArgumentException("email must be 150 chars or less");
        if (email != null && !email.contains("@"))
            throw new IllegalArgumentException("email must be a valid email address");
        if (address != null && address.length() > 200)
            throw new IllegalArgumentException("address must be 200 chars or less");
        if (paymentTermsDays != null && paymentTermsDays < 0)
            throw new IllegalArgumentException("paymentTermsDays cannot be negative");
        if (notes != null && notes.length() > 500)
            throw new IllegalArgumentException("notes must be 500 chars or less");
        if (company == null)
            throw new IllegalArgumentException("company is required");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public String getNotes() {
        return notes;
    }

    public CompanyRef getCompany() {
        return company;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public Long getUpdatedBy() {
        return updatedBy;
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
