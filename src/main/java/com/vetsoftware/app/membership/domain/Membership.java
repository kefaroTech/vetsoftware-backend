package com.vetsoftware.app.membership.domain;

import java.time.LocalDateTime;

public class Membership {
    private Long id;
    private String name;
    private MembershipStatus status;
    private final LocalDateTime createdDate;
    private final Long createdBy;

    public Membership(Long id, String name, MembershipStatus status, LocalDateTime createdDate, Long createdBy) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdDate = createdDate;
        this.createdBy = createdBy;
    }

    public static Membership create(String name, MembershipStatus status, Long createdBy) {
        return new Membership(null, name, status, LocalDateTime.now(), createdBy);
    }

    public void update(String name, MembershipStatus status) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.name = name;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public MembershipStatus getStatus() { return status; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public Long getCreatedBy() { return createdBy; }
}
