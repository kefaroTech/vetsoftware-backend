package com.vetsoftware.app.platformaccess.domain;

/**
 * Desenlace de una solicitud de alta de superadministrador. Se persiste como
 * texto en {@code platform_access_requests.decision} con un {@code CHECK}, no
 * como {@code ENUM} de MySQL: añadir un valor sería un {@code ALTER} de tabla.
 */
public enum PlatformAccessDecision {

    APPROVED, REJECTED;

    /** {@code null} si el texto no corresponde a ninguna decisión conocida. */
    public static PlatformAccessDecision fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PlatformAccessDecision decision : values()) {
            if (decision.name().equals(value)) {
                return decision;
            }
        }
        throw new IllegalArgumentException("Unknown platform access decision");
    }
}
