package com.vetsoftware.app.clinicalhistory.application.dto;

/**
 * Alerta / alergia / nota cautelar del paciente, para el banner de seguridad
 * del reporte PDF.
 */
public record ReportAlert(String typeLabel, String description, String severityLabel) {
}
