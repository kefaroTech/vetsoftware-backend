package com.vetsoftware.app.clinicalhistory.application.dto;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.util.List;

/**
 * Evento clínico enriquecido para el reporte PDF: además del encabezado del timeline, lleva los
 * campos ricos (formato SOAP) y las tablas (medicamentos, medicaciones…) cargados desde la entidad
 * fuente.
 */
public record ReportClinicalEvent(
    Long sourceId,
    ClinicalEventType eventType,
    LocalDate eventDate,
    LocalDate endDate,
    Long consultationId,
    String title,
    List<DetailField> fields,
    List<DetailTable> tables) {}
