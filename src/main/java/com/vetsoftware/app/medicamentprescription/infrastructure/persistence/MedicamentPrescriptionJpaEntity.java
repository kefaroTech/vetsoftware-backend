package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicament_prescriptions")
@SQLDelete(sql = "UPDATE medicament_prescriptions SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class MedicamentPrescriptionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String presentation;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false, length = 1000)
    private String posology;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private PrescriptionJpaEntity prescription;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected MedicamentPrescriptionJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPresentation() { return presentation; }
    public void setPresentation(String presentation) { this.presentation = presentation; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public String getPosology() { return posology; }
    public void setPosology(String posology) { this.posology = posology; }
    public PrescriptionJpaEntity getPrescription() { return prescription; }
    public void setPrescription(PrescriptionJpaEntity prescription) { this.prescription = prescription; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
