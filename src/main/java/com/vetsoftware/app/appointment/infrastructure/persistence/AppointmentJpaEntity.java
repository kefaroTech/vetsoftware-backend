package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * Cita. La columna generada {@code active_slot_employee_id} (changeset 226,
 * reexpresada por el changeset de la issue #240; = employee_id solo cuando
 * enabled=true, status no está en CANCELLED/NO_SHOW y overlap_forced=false,
 * NULL en otro caso) vive en la BD para el índice único
 * {@code uq_appointments_active_employee_start} -issue #114, el solape EXACTO
 * de horario-; no se mapea aquí (ddl-auto: validate ignora columnas no
 * mapeadas), mismo patrón que {@code CashSessionJpaEntity.open_marker}.
 *
 * <p>
 * {@code overlap_forced} SÍ se mapea, y es la única entrada que tiene el código
 * a esa columna generada: poniéndolo a true la fila deja de reservar su hueco y
 * puede convivir con otra a la misma hora. Es lo que distingue el doble booking
 * deliberado de la carrera concurrente (issue #240).
 */
@Entity
@Table(name = "appointments")
@SQLDelete(sql = "UPDATE appointments SET enabled = false WHERE id = ? AND version = ?")
@SQLRestriction("enabled = true")
public class AppointmentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    // NULLABLE a propósito (changeset 222): null = la cita hereda la duración por
    // defecto de la empresa. Integer envuelto, no int: con ddl-auto validate un
    // primitivo sobre columna nullable es una bomba de NPE al leer filas antiguas.
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id")
    private AnimalJpaEntity animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private OwnerJpaEntity owner;

    @Column(name = "client_name", length = 120)
    private String clientName;

    @Column(name = "client_phone", length = 30)
    private String clientPhone;

    @Column(name = "client_email", length = 150)
    private String clientEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeJpaEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyJpaEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchJpaEntity branch;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    // Mismo tipo de columna que `enabled` (BOOLEAN de Liquibase, 174:42): con
    // ddl-auto validate un TINYINT(1) escrito a mano se reporta como BIT y no casa
    // con el boolean de Hibernate.
    @Column(name = "overlap_forced", nullable = false)
    private boolean overlapForced;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected AppointmentJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public AnimalJpaEntity getAnimal() {
        return animal;
    }

    public void setAnimal(AnimalJpaEntity animal) {
        this.animal = animal;
    }

    public OwnerJpaEntity getOwner() {
        return owner;
    }

    public void setOwner(OwnerJpaEntity owner) {
        this.owner = owner;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public EmployeeJpaEntity getEmployee() {
        return employee;
    }

    public void setEmployee(EmployeeJpaEntity employee) {
        this.employee = employee;
    }

    public CompanyJpaEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyJpaEntity company) {
        this.company = company;
    }

    public BranchJpaEntity getBranch() {
        return branch;
    }

    public void setBranch(BranchJpaEntity branch) {
        this.branch = branch;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOverlapForced() {
        return overlapForced;
    }

    public void setOverlapForced(boolean overlapForced) {
        this.overlapForced = overlapForced;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
