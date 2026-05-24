package com.vetsoftware.app.clinicalhistory.infrastructure.persistence;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_clinical_event")
public class ClinicalEventViewJpaEntity {

    @Id
    @Column(name = "composite_key", nullable = false, length = 64)
    private String compositeKey;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "consultation_id")
    private Long consultationId;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private ClinicalEventType eventType;

    @Column(name = "summary", length = 255)
    private String summary;

    protected ClinicalEventViewJpaEntity() {}

    public String getCompositeKey() { return compositeKey; }
    public Long getSourceId() { return sourceId; }
    public Long getAnimalId() { return animalId; }
    public Long getCompanyId() { return companyId; }
    public Long getConsultationId() { return consultationId; }
    public LocalDate getEventDate() { return eventDate; }
    public ClinicalEventType getEventType() { return eventType; }
    public String getSummary() { return summary; }
}
