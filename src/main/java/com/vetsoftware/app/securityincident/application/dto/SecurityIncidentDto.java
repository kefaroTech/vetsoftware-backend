package com.vetsoftware.app.securityincident.application.dto;

import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code version}</strong>: el numero de bloqueo optimista es una
 * barandilla del que escribe, no un dato del incidente. Publicarlo invitaria a
 * devolverlo y a construir un protocolo de concurrencia que una tabla que solo
 * escribe plataforma no necesita.
 */
public record SecurityIncidentDto(Long id, LocalDateTime detectedAt, LocalDateTime occurredAt,
        LocalDateTime escalatedAt, SecurityIncidentKind kind, IncidentSeverity severity,
        String summary, int affectedSubjectCount, LocalDateTime deadlineAt,
        LocalDateTime reportedToAuthorityAt, String reportReference,
        LocalDateTime notifiedSubjectsAt, String containment, String rootCause,
        LocalDateTime closedAt, LocalDateTime createdDate) {

    public static SecurityIncidentDto from(SecurityIncident incident) {
        return new SecurityIncidentDto(incident.getId(), incident.getDetectedAt(),
                incident.getOccurredAt(), incident.getEscalatedAt(), incident.getKind(),
                incident.getSeverity(), incident.getSummary(), incident.getAffectedSubjectCount(),
                incident.getDeadlineAt(), incident.getReportedToAuthorityAt(),
                incident.getReportReference(), incident.getNotifiedSubjectsAt(),
                incident.getContainment(), incident.getRootCause(), incident.getClosedAt(),
                incident.getCreatedDate());
    }
}
