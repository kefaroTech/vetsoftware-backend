package com.vetsoftware.app.company.application.port.out;

/**
 * Rastro de auditoria de los hechos del registro de empresas.
 *
 * <p>
 * Restaurar una empresa devuelve el acceso a todos sus empleados de una sola
 * vez: es un cambio de alcance de permisos, no una edicion de ficha, y el
 * evento generico {@code http_mutation} del borde HTTP solo deja constancia de
 * que hubo un {@code PATCH} con un {@code 200}. Quien reclame por que una
 * clinica volvio a estar operativa necesita el id y el nombre en el evento, que
 * es lo que este puerto añade.
 *
 * <p>
 * Actor, empresa y origen viajan por el MDC — ver {@code AuditLogger}.
 */
public interface CompanyAuditPort {

    void companyReactivated(Long companyId, String companyName, String companyIdentifier);
}
