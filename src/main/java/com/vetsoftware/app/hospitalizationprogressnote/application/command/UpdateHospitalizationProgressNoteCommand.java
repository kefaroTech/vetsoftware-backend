package com.vetsoftware.app.hospitalizationprogressnote.application.command;

/**
 * El {@code companyId} no viaja en el request REST: lo pone el controller desde
 * el contexto autenticado y el {@code @PreAuthorize} del puerto lo revalida.
 */
public record UpdateHospitalizationProgressNoteCommand(Long id, String description,
        Long companyId) {
}
