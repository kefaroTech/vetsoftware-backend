package com.vetsoftware.app.domain;

import java.time.LocalDateTime;

public class Company {
    private final CompanyId id;
    private String name;
    private String identifier;
    private String address;
    private String contactNumber;
    private final LocalDateTime createdDate;
    private final String createdBy;

    public Company(CompanyId id, String name, String identifier, String address,
                   String contactNumber, LocalDateTime createdDate, String createdBy) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (identifier == null || identifier.isBlank()) throw new IllegalArgumentException("identifier is required");
        if (identifier.length() > 50) throw new IllegalArgumentException("identifier must be 50 chars or less");
        this.id = id;
        this.name = name;
        this.identifier = identifier;
        this.address = address;
        this.contactNumber = contactNumber;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
    }

    public static Company create(String name, String identifier, String address, String contactNumber, String createdBy) {
        return new Company(CompanyId.generate(), name, identifier, address, contactNumber, LocalDateTime.now(), createdBy);
    }

    public void update(String name, String identifier, String address, String contactNumber) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (identifier == null || identifier.isBlank()) throw new IllegalArgumentException("identifier is required");
        if (identifier.length() > 50) throw new IllegalArgumentException("identifier must be 50 chars or less");
        this.name = name;
        this.identifier = identifier;
        this.address = address;
        this.contactNumber = contactNumber;
    }

    public CompanyId getId() { return id; }
    public String getName() { return name; }
    public String getIdentifier() { return identifier; }
    public String getAddress() { return address; }
    public String getContactNumber() { return contactNumber; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public String getCreatedBy() { return createdBy; }
}
