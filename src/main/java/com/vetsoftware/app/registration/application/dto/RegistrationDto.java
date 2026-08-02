package com.vetsoftware.app.registration.application.dto;

/**
 * Resultado del auto-registro (Opción B). NO incluye token de sesión: la cuenta queda pendiente de
 * verificar el correo antes de poder iniciar sesión. El usuario de acceso es el propio {@code
 * email} del administrador. {@code status} = PENDING_VERIFICATION.
 */
public record RegistrationDto(Long companyId, Long employeeId, String email, String status) {}
