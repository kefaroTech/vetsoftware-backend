package com.vetsoftware.app.spatype.domain;

/**
 * El nombre de un tipo del catálogo es único. La base lo impone con un índice
 * único que cubre solo las filas activas; esta excepción es la detección
 * temprana del mismo choque, en español y atribuible al campo.
 *
 * <p>
 * No sustituye al mapeo de la constraint en
 * {@code GlobalExceptionHandler.mapConstraint}: una guarda previa no cierra la
 * carrera entre dos altas simultáneas (#437). Los dos caminos emiten el mismo
 * {@code errorCode} a propósito — al front le da igual quién detectó el choque.
 */
public class SpaTypeNameAlreadyExistsException extends RuntimeException {
    public SpaTypeNameAlreadyExistsException(String name) {
        super("Ya existe un tipo de spa activo con el nombre '" + name + "' en este catálogo.");
    }
}
