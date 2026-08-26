package com.vetsoftware.app.medicament.application.command;

/**
 * Edicion en el vademecum de PLATAFORMA. Sin {@code companyId} por el mismo
 * motivo que {@link CreateGlobalMedicamentCommand}: el ambito de este caso de
 * uso es una constante, no un dato de entrada.
 */
public record UpdateGlobalMedicamentCommand(Long id, String name, String description) {
}
