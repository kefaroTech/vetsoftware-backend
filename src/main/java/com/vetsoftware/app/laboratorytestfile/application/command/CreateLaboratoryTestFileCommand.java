package com.vetsoftware.app.laboratorytestfile.application.command;

/**
 * El {@code companyId} no viene del formulario: lo pone el controller desde el
 * principal y el puerto de entrada lo revalida con {@code @authz.isMyCompany}.
 * Esta aqui porque sin el no hay con que acotar el examen de laboratorio padre,
 * que es lo que impide subir un adjunto al resultado de otro tenant.
 */
public record CreateLaboratoryTestFileCommand(Long laboratoryTestId, String originalFileName,
        String contentType, long sizeBytes, byte[] content, Long uploadedById, Long companyId) {
}
