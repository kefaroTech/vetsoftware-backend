package com.vetsoftware.app.hospitalizationobservation.application.command;

/**
 * El {@code companyId} no viaja en el request REST: lo pone el controller desde
 * el contexto autenticado y el {@code @PreAuthorize} del puerto lo revalida.
 */
public record UpdateHospitalizationObservationCommand(Long id, String description, Long companyId) {
}
