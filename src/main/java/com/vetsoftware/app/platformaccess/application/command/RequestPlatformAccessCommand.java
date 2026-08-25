package com.vetsoftware.app.platformaccess.application.command;

/**
 * Datos del formulario público de solicitud de acceso de plataforma. No lleva
 * {@code companyId}: el flujo es global y ninguna de sus tablas tiene empresa.
 */
public record RequestPlatformAccessCommand(String fullName, String email, String reason) {
}
