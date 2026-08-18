package com.vetsoftware.app.laboratorytest.application.command;

/**
 * Cambio de estado de una muestra. Ni el {@code processedById} ni el
 * {@code companyId} viajan en el request REST: los pone el controller desde el
 * contexto autenticado. El {@code processedById} es la <b>firma</b> de quien
 * valida (autoría); el {@code companyId} es el que acota la carga de la muestra
 * y el que revalida el {@code @PreAuthorize} del puerto. {@code null} en
 * {@code companyId} es el principal SYSTEM, que no tiene empresa.
 */
public record ChangeLaboratoryTestStatusCommand(Long id, String status, Long processedById,
        Long companyId) {
}
