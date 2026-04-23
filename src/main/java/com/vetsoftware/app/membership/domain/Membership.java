package com.vetsoftware.app.membership.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Membership {
    private Long id;
    private String name;
    private MembershipStatus status;
    private final LocalDateTime createdDate;
    private List<Long> moduleIds;

    public Membership(Long id, String name, MembershipStatus status, LocalDateTime createdDate, List<Long> moduleIds) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.id = id;
        this.name = name;
        this.status = status;
        this.createdDate = createdDate;
        this.moduleIds = moduleIds != null ? new ArrayList<>(moduleIds) : new ArrayList<>();
    }

    public static Membership create(String name, MembershipStatus status, List<Long> moduleIds) {
        return new Membership(null, name, status, LocalDateTime.now(), moduleIds);
    }

    public void update(String name, MembershipStatus status, List<Long> moduleIds) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (name.length() > 100) throw new IllegalArgumentException("name must be 100 chars or less");
        if (status == null) throw new IllegalArgumentException("status is required");
        this.name = name;
        this.status = status;
        this.moduleIds = moduleIds != null ? new ArrayList<>(moduleIds) : new ArrayList<>();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public MembershipStatus getStatus() { return status; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public List<Long> getModuleIds() { return moduleIds; }
}
