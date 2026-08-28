package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Copia la version en los dos sentidos, y de eso depende que reportar y
 * cerrar sean ediciones y no inserts.</strong> Si {@code toJpa} dejara la
 * version en {@code null} sobre una entidad que ya tiene id, Hibernate la
 * tomaria por transitoria y el {@code merge} escribiria una fila nueva: dos
 * incidentes donde hay uno, y el reporte anotado sobre el que nadie mira.
 */
@Component
public class SecurityIncidentJpaMapper {

    public SecurityIncidentJpaEntity toJpa(SecurityIncident incident) {
        SecurityIncidentJpaEntity entity = new SecurityIncidentJpaEntity();
        entity.setId(incident.getId());
        entity.setDetectedAt(incident.getDetectedAt());
        entity.setOccurredAt(incident.getOccurredAt());
        entity.setEscalatedAt(incident.getEscalatedAt());
        entity.setKind(incident.getKind());
        entity.setSeverity(incident.getSeverity());
        entity.setSummary(incident.getSummary());
        entity.setAffectedSubjectCount(incident.getAffectedSubjectCount());
        entity.setDeadlineAt(incident.getDeadlineAt());
        entity.setReportedToAuthorityAt(incident.getReportedToAuthorityAt());
        entity.setReportReference(incident.getReportReference());
        entity.setNotifiedSubjectsAt(incident.getNotifiedSubjectsAt());
        entity.setContainment(incident.getContainment());
        entity.setRootCause(incident.getRootCause());
        entity.setClosedAt(incident.getClosedAt());
        entity.setCreatedDate(incident.getCreatedDate());
        entity.setVersion(incident.getVersion());
        return entity;
    }

    public SecurityIncident toDomain(SecurityIncidentJpaEntity entity) {
        return new SecurityIncident(entity.getId(), entity.getDetectedAt(), entity.getOccurredAt(),
                entity.getEscalatedAt(), entity.getKind(), entity.getSeverity(),
                entity.getSummary(), entity.getAffectedSubjectCount(), entity.getDeadlineAt(),
                entity.getReportedToAuthorityAt(), entity.getReportReference(),
                entity.getNotifiedSubjectsAt(), entity.getContainment(), entity.getRootCause(),
                entity.getClosedAt(), entity.getCreatedDate(), entity.getVersion());
    }
}
