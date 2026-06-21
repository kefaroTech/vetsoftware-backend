package com.vetsoftware.app.openaccount.domain;

/**
 * La versión esperada por el cliente no coincide con la versión actual de la cuenta:
 * otra operación la modificó entre la lectura del front y esta mutación. Detección temprana
 * del conflicto optimista (antes de tocar datos), equivalente al {@code @Version} de Hibernate
 * pero accionable por el front sin esperar al flush.
 */
public class OpenAccountVersionConflictException extends RuntimeException {
    public OpenAccountVersionConflictException(Long openAccountId, Long expected, Long actual) {
        super("Open account " + openAccountId + " version conflict: expected " + expected
            + " but current is " + actual);
    }
}
