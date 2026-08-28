package com.vetsoftware.app.securityincident.domain;

/**
 * De que clase fue el incidente.
 *
 * <p>
 * Espejo exacto de {@code chk_security_incidents_kind} (changeset 356). No es
 * texto libre porque la clase es lo primero que pregunta la autoridad y lo que
 * decide como se agrupan los incidentes en el informe: dos formas de escribir
 * «acceso no autorizado» son dos filas que no se suman.
 */
public enum SecurityIncidentKind {
    /** Alguien entro donde no debia. */
    UNAUTHORIZED_ACCESS,
    /** Se perdio informacion: borrado, corrupcion, respaldo ilegible. */
    DATA_LOSS,
    /** La informacion salio: quedo expuesta o se filtro. */
    DATA_LEAK,
    /** Cifrado hostil con extorsion. */
    RANSOMWARE,
    /** Uso abusivo del servicio por un tercero. */
    SERVICE_ABUSE,
    /** Cualquier otro. El {@code summary} es entonces lo unico que explica. */
    OTHER
}
