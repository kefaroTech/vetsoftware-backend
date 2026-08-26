package com.vetsoftware.app.medicament.domain;

/**
 * El nombre de un medicamento es único dentro de su ÁMBITO: la empresa para uno
 * propio, el vademécum de plataforma para uno general. La base lo impone con un
 * índice único que cubre solo las filas activas; esta excepción es la detección
 * temprana del mismo choque, en español y atribuible al campo.
 *
 * <p>
 * No sustituye al mapeo de la constraint en
 * {@code GlobalExceptionHandler.mapConstraint}: una guarda previa no cierra la
 * carrera entre dos altas simultáneas (#437). Los dos caminos emiten el mismo
 * {@code errorCode} a propósito — al front le da igual quién detectó el choque.
 */
public class MedicamentNameAlreadyExistsException extends RuntimeException {
    public MedicamentNameAlreadyExistsException(String name) {
        super("Ya existe un medicamento activo con el nombre '" + name + "' en este ámbito.");
    }
}
