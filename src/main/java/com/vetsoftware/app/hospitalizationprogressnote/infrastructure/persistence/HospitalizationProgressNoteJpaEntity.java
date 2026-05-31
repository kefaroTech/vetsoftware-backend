package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "hospitalization_progress_notes")
@SQLDelete(sql = "UPDATE hospitalization_progress_notes SET enabled = false WHERE id = ?")
@SQLRestriction("enabled = true")
public class HospitalizationProgressNoteJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospitalization_id", nullable = false)
    private HospitalizationJpaEntity hospitalization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private EmployeeJpaEntity createdBy;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected HospitalizationProgressNoteJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public HospitalizationJpaEntity getHospitalization() { return hospitalization; }
    public void setHospitalization(HospitalizationJpaEntity hospitalization) { this.hospitalization = hospitalization; }
    public EmployeeJpaEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(EmployeeJpaEntity createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
