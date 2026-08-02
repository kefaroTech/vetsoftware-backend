package com.vetsoftware.app.appointment.domain;

import java.time.LocalDateTime;

public class Appointment {
  private Long id;
  private LocalDateTime startAt;
  private AppointmentType type;
  private AppointmentStatus status;
  private String notes;
  private String cancellationReason;
  private AnimalRef animal; // opcional
  private OwnerRef owner; // opcional
  private String clientName; // opcional (contacto libre)
  private String clientPhone; // opcional
  private String clientEmail; // opcional (para notificar al contacto libre)
  private EmployeeRef employee; // requerido (vet asignado)
  private CompanyRef company; // requerido
  private BranchRef branch; // requerido (sede de la cita)
  private long version;
  private boolean enabled;
  private final LocalDateTime createdDate;

  public Appointment(
      Long id,
      LocalDateTime startAt,
      AppointmentType type,
      AppointmentStatus status,
      String notes,
      String cancellationReason,
      AnimalRef animal,
      OwnerRef owner,
      String clientName,
      String clientPhone,
      String clientEmail,
      EmployeeRef employee,
      CompanyRef company,
      BranchRef branch,
      long version,
      boolean enabled,
      LocalDateTime createdDate) {
    validate(
        startAt,
        type,
        employee,
        company,
        branch,
        animal,
        owner,
        clientName,
        notes,
        clientPhone,
        clientEmail,
        cancellationReason);
    this.id = id;
    this.startAt = startAt;
    this.type = type;
    this.status = status == null ? AppointmentStatus.REQUESTED : status;
    this.notes = blankToNull(notes);
    this.cancellationReason = blankToNull(cancellationReason);
    this.animal = animal;
    this.owner = owner;
    this.clientName = blankToNull(clientName);
    this.clientPhone = blankToNull(clientPhone);
    this.clientEmail = blankToNull(clientEmail);
    this.employee = employee;
    this.company = company;
    this.branch = branch;
    this.version = version;
    this.enabled = enabled;
    this.createdDate = createdDate;
  }

  public static Appointment create(
      LocalDateTime startAt,
      AppointmentType type,
      String notes,
      AnimalRef animal,
      OwnerRef owner,
      String clientName,
      String clientPhone,
      String clientEmail,
      EmployeeRef employee,
      CompanyRef company,
      BranchRef branch) {
    return new Appointment(
        null,
        startAt,
        type,
        AppointmentStatus.REQUESTED,
        notes,
        null,
        animal,
        owner,
        clientName,
        clientPhone,
        clientEmail,
        employee,
        company,
        branch,
        0L,
        true,
        LocalDateTime.now());
  }

  public void update(
      LocalDateTime startAt,
      AppointmentType type,
      String notes,
      AnimalRef animal,
      OwnerRef owner,
      String clientName,
      String clientPhone,
      String clientEmail,
      EmployeeRef employee) {
    validate(
        startAt,
        type,
        employee,
        this.company,
        this.branch,
        animal,
        owner,
        clientName,
        notes,
        clientPhone,
        clientEmail,
        this.cancellationReason);
    this.startAt = startAt;
    this.type = type;
    this.notes = blankToNull(notes);
    this.animal = animal;
    this.owner = owner;
    this.clientName = blankToNull(clientName);
    this.clientPhone = blankToNull(clientPhone);
    this.clientEmail = blankToNull(clientEmail);
    this.employee = employee;
  }

  public void reschedule(LocalDateTime startAt, EmployeeRef employee) {
    if (startAt == null) throw new IllegalArgumentException("startAt is required");
    if (employee == null) throw new IllegalArgumentException("employee is required");
    this.startAt = startAt;
    this.employee = employee;
  }

  public void transitionTo(AppointmentStatus next) {
    if (next == null) throw new IllegalArgumentException("status is required");
    if (next == AppointmentStatus.CANCELLED) {
      cancel(this.cancellationReason);
      return;
    }
    if (!status.canTransitionTo(next)) {
      throw new InvalidAppointmentTransitionException(status, next);
    }
    this.status = next;
  }

  public void cancel(String reason) {
    if (!status.canTransitionTo(AppointmentStatus.CANCELLED)) {
      throw new InvalidAppointmentTransitionException(status, AppointmentStatus.CANCELLED);
    }
    if (reason != null && reason.length() > 300) {
      throw new IllegalArgumentException("cancellationReason must be 300 chars or less");
    }
    this.status = AppointmentStatus.CANCELLED;
    this.cancellationReason = blankToNull(reason);
  }

  private static void validate(
      LocalDateTime startAt,
      AppointmentType type,
      EmployeeRef employee,
      CompanyRef company,
      BranchRef branch,
      AnimalRef animal,
      OwnerRef owner,
      String clientName,
      String notes,
      String clientPhone,
      String clientEmail,
      String cancellationReason) {
    if (startAt == null) throw new IllegalArgumentException("startAt is required");
    if (type == null) throw new IllegalArgumentException("type is required");
    if (employee == null) throw new IllegalArgumentException("employee is required");
    if (company == null) throw new IllegalArgumentException("company is required");
    if (branch == null) throw new IllegalArgumentException("branch is required");
    boolean hasSubject =
        animal != null || owner != null || (clientName != null && !clientName.isBlank());
    if (!hasSubject) {
      throw new IllegalArgumentException("at least one of {animal, owner, clientName} is required");
    }
    if (notes != null && notes.length() > 1000)
      throw new IllegalArgumentException("notes must be 1000 chars or less");
    if (clientName != null && clientName.length() > 120)
      throw new IllegalArgumentException("clientName must be 120 chars or less");
    if (clientPhone != null && clientPhone.length() > 30)
      throw new IllegalArgumentException("clientPhone must be 30 chars or less");
    if (clientEmail != null && clientEmail.length() > 150)
      throw new IllegalArgumentException("clientEmail must be 150 chars or less");
    if (cancellationReason != null && cancellationReason.length() > 300)
      throw new IllegalArgumentException("cancellationReason must be 300 chars or less");
  }

  private static String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  public Long getId() {
    return id;
  }

  public LocalDateTime getStartAt() {
    return startAt;
  }

  public AppointmentType getType() {
    return type;
  }

  public AppointmentStatus getStatus() {
    return status;
  }

  public String getNotes() {
    return notes;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public AnimalRef getAnimal() {
    return animal;
  }

  public OwnerRef getOwner() {
    return owner;
  }

  public String getClientName() {
    return clientName;
  }

  public String getClientPhone() {
    return clientPhone;
  }

  public String getClientEmail() {
    return clientEmail;
  }

  public EmployeeRef getEmployee() {
    return employee;
  }

  public CompanyRef getCompany() {
    return company;
  }

  public BranchRef getBranch() {
    return branch;
  }

  public long getVersion() {
    return version;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }
}
