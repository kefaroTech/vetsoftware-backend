package com.vetsoftware.app.branch.domain;

import java.time.LocalDateTime;

/**
 * Sucursal (sede) de una empresa. Es el ancla del multi-sucursal: los recursos
 * operativos (stock, caja, ventas, numeración fiscal, citas) se scopean a una
 * {@code Branch}. {@code active} es un estado de negocio (operativa / no
 * operativa), independiente del borrado — una sucursal inactiva sigue visible
 * para poder reactivarla y para no perder el histórico que la referencia.
 */
public class Branch {
    private Long id;
    private String name;
    private String code;
    private String address;
    private String phone;
    private CityRef city;
    private final CompanyRef company;
    private final LocalDateTime createdDate;
    private Long version;
    private boolean active;

    public Branch(Long id, String name, String code, String address, String phone, CityRef city,
            CompanyRef company, LocalDateTime createdDate, Long version, boolean active) {
        validate(name, code, address, phone, city, company);
        this.id = id;
        this.name = name;
        this.code = code;
        this.address = address;
        this.phone = phone;
        this.city = city;
        this.company = company;
        this.createdDate = createdDate;
        this.version = version;
        this.active = active;
    }

    public static Branch create(String name, String code, String address, String phone,
            CityRef city, CompanyRef company) {
        return new Branch(null, name, code, address, phone, city, company, LocalDateTime.now(),
                null, true);
    }

    public void update(String name, String code, String address, String phone, CityRef city) {
        validate(name, code, address, phone, city, this.company);
        this.name = name;
        this.code = code;
        this.address = address;
        this.phone = phone;
        this.city = city;
    }

    /** Reactiva la sucursal (vuelve a estar operativa). Idempotente. */
    public void activate() {
        this.active = true;
    }

    /**
     * Desactiva la sucursal (deja de estar operativa, pero no se borra).
     * Idempotente.
     */
    public void deactivate() {
        this.active = false;
    }

    private static void validate(String name, String code, String address, String phone,
            CityRef city, CompanyRef company) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 120)
            throw new IllegalArgumentException("name must be 120 chars or less");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("code is required");
        if (code.length() > 30)
            throw new IllegalArgumentException("code must be 30 chars or less");
        if (address != null && address.length() > 255)
            throw new IllegalArgumentException("address must be 255 chars or less");
        if (phone != null && phone.length() > 30)
            throw new IllegalArgumentException("phone must be 30 chars or less");
        if (city == null)
            throw new IllegalArgumentException("city is required");
        if (company == null)
            throw new IllegalArgumentException("company is required");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isActive() {
        return active;
    }
}
