package com.vetsoftware.app.hospitalizationprocedure.application.command;

/**
 * El {@code companyId} no viaja en el request REST: lo pone el controller desde
 * el contexto autenticado y el {@code @PreAuthorize} del puerto lo revalida.
 */
public record SuspendHospitalizationProcedureCommand(Long id, Long suspendedById, Long companyId) {
}
