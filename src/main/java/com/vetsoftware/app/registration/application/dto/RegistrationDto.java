package com.vetsoftware.app.registration.application.dto;

/**
 * Resultado del auto-registro (Opcion B). NO incluye token de sesion: la cuenta queda pendiente de
 * verificar el correo antes de poder iniciar sesion. {@code status} = PENDING_VERIFICATION.
 */
public record RegistrationDto(Long companyId, Long employeeId, String email, String status) {}
