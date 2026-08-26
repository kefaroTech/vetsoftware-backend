package com.vetsoftware.app.medicament.application.command;

/**
 * Alta en el vademecum de PLATAFORMA. No lleva {@code companyId} ni
 * {@code general} a proposito, al contrario que
 * {@link CreateMedicamentCommand}: los dos son constantes de este caso de uso
 * —empresa nula y {@code general = true}— y los pone el servidor. Un campo mas
 * en el command seria un campo que alguien puede rellenar desde el request, y
 * el XOR del dominio ya no seria una invariante sino una esperanza.
 */
public record CreateGlobalMedicamentCommand(String name, String description) {
}
