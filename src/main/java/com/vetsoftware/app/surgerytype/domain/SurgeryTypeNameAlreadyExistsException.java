package com.vetsoftware.app.surgerytype.domain;

/**
 * El nombre es único dentro de su ÁMBITO: la empresa para un tipo propio, el
 * catálogo de plataforma para uno global. La base lo impone con un índice único
 * que cubre solo las filas activas; esta excepción es la detección temprana del
 * mismo choque, en español y atribuible al campo.
 *
 * <p>
 * No sustituye al mapeo de la constraint en
 * {@code GlobalExceptionHandler.mapConstraint}: una guarda previa no cierra la
 * carrera entre dos altas simultáneas (#437). Los dos caminos emiten el mismo
 * {@code errorCode} a propósito — al front le da igual quién detectó el choque.
 */
public class SurgeryTypeNameAlreadyExistsException extends RuntimeException {
    public SurgeryTypeNameAlreadyExistsException(String name) {
        super("Ya existe un tipo de cirugía activo con el nombre '" + name + "' en este ámbito.");
    }
}
