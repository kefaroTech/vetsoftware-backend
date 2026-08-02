package com.vetsoftware.app.clinicalhistory.application.dto;

/**
 * Un campo etiquetado del detalle clínico de un evento (p. ej. "Anamnesis" →
 * texto). El adaptador de PDF lo renderiza como bloque estructurado tipo SOAP.
 */
public record DetailField(String label, String value) {
}
