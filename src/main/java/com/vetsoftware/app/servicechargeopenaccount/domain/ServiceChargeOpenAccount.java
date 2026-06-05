package com.vetsoftware.app.servicechargeopenaccount.domain;

import java.time.LocalDateTime;

public class ServiceChargeOpenAccount {
    private Long id;
    private AnimalRef animal;
    private ServiceRef service;
    private OpenAccountRef openAccount;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public ServiceChargeOpenAccount(Long id, AnimalRef animal, ServiceRef service,
                                    OpenAccountRef openAccount, EmployeeRef createdBy,
                                    LocalDateTime createdDate, boolean enabled) {
        validate(animal, service, openAccount);
        this.id = id;
        this.animal = animal;
        this.service = service;
        this.openAccount = openAccount;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static ServiceChargeOpenAccount create(AnimalRef animal, ServiceRef service,
                                                  OpenAccountRef openAccount, EmployeeRef createdBy) {
        return new ServiceChargeOpenAccount(null, animal, service, openAccount, createdBy,
                                            LocalDateTime.now(), true);
    }

    public void update(AnimalRef animal, ServiceRef service, OpenAccountRef openAccount) {
        validate(animal, service, openAccount);
        this.animal = animal;
        this.service = service;
        this.openAccount = openAccount;
    }

    private static void validate(AnimalRef animal, ServiceRef service, OpenAccountRef openAccount) {
        if (animal == null) throw new IllegalArgumentException("animal is required");
        if (service == null) throw new IllegalArgumentException("service is required");
        if (openAccount == null) throw new IllegalArgumentException("openAccount is required");
    }

    public Long getId() { return id; }
    public AnimalRef getAnimal() { return animal; }
    public ServiceRef getService() { return service; }
    public OpenAccountRef getOpenAccount() { return openAccount; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }
}
