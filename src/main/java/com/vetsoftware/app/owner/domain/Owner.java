package com.vetsoftware.app.owner.domain;

import java.time.LocalDateTime;

public class Owner {
    private Long id;
    private String name;
    private String email;
    private String document;
    private String address;
    private String phone;
    private CityRef city;
    private CompanyRef company;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public Owner(Long id, String name, String email, String document, String address,
                 String phone, CityRef city, CompanyRef company, LocalDateTime createdDate,
                 boolean enabled) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 150) throw new IllegalArgumentException("name must be 150 chars or less");
        if (document == null || document.isBlank()) throw new IllegalArgumentException("document is required");
        if (document.length() > 50) throw new IllegalArgumentException("document must be 50 chars or less");
        if (email != null && email.length() > 150) throw new IllegalArgumentException("email must be 150 chars or less");
        if (address != null && address.length() > 255) throw new IllegalArgumentException("address must be 255 chars or less");
        if (phone != null && phone.length() > 30) throw new IllegalArgumentException("phone must be 30 chars or less");
        if (city == null) throw new IllegalArgumentException("city is required");
        if (company == null) throw new IllegalArgumentException("company is required");
        this.id = id;
        this.name = name;
        this.email = email;
        this.document = document;
        this.address = address;
        this.phone = phone;
        this.city = city;
        this.company = company;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static Owner create(String name, String email, String document, String address,
                                String phone, CityRef city, CompanyRef company) {
        return new Owner(null, name, email, document, address, phone, city, company, LocalDateTime.now(), true);
    }

    public void update(String name, String email, String document, String address,
                       String phone, CityRef city, CompanyRef company) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 150) throw new IllegalArgumentException("name must be 150 chars or less");
        if (document == null || document.isBlank()) throw new IllegalArgumentException("document is required");
        if (document.length() > 50) throw new IllegalArgumentException("document must be 50 chars or less");
        if (email != null && email.length() > 150) throw new IllegalArgumentException("email must be 150 chars or less");
        if (address != null && address.length() > 255) throw new IllegalArgumentException("address must be 255 chars or less");
        if (phone != null && phone.length() > 30) throw new IllegalArgumentException("phone must be 30 chars or less");
        if (city == null) throw new IllegalArgumentException("city is required");
        if (company == null) throw new IllegalArgumentException("company is required");
        this.name = name;
        this.email = email;
        this.document = document;
        this.address = address;
        this.phone = phone;
        this.city = city;
        this.company = company;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDocument() { return document; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public CityRef getCity() { return city; }
    public CompanyRef getCompany() { return company; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
