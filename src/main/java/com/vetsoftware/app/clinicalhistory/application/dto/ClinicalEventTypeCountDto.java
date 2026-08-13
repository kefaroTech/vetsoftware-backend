package com.vetsoftware.app.clinicalhistory.application.dto;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;

/**
 * Cuantos eventos de un tipo tiene el animal en toda su historia.
 *
 * <p>
 * BE-06: los chips de la pantalla mostraban este contador recorriendo el array
 * completo. Paginada, esa cuenta seria la de lo scrolleado, no la del paciente.
 */
public record ClinicalEventTypeCountDto(ClinicalEventType eventType, long count) {
}
