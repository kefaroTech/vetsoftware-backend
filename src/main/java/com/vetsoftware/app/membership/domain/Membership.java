package com.vetsoftware.app.membership.domain;

import java.time.LocalDateTime;

public class Membership {
    private Long id;
    private String name;
    private MembershipStatus status;
    private boolean mandatory;
    private final LocalDateTime createdDate;

    public Membership(Long id, String name, MembershipStatus status, boolean mandatory,
                      LocalDateTime createdDate) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.id = id;
        this.name = name;
        this.status = status;
        this.mandatory = mandatory;
        this.createdDate = createdDate;
    }

    public static Membership create(String name, MembershipStatus status, boolean mandatory) {
        return new Membership(null, name, status, mandatory, LocalDateTime.now());
    }

    public void update(String name, MembershipStatus status, boolean mandatory) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.name = name;
        this.status = status;
        this.mandatory = mandatory;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public MembershipStatus getStatus() { return status; }
    public boolean isMandatory() { return mandatory; }
    public LocalDateTime getCreatedDate() { return createdDate; }
}
