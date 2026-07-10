package com.vetsoftware.app.registration.application.dto;

/**
 * Resultado del auto-registro (Opción B). NO incluye token de sesión: la cuenta queda pendiente de
 * verificar el correo antes de poder iniciar sesión. {@code employeeCode} es el usuario de acceso generado
 * (se muestra al dueño para que sepa con qué iniciar sesión). {@code status} = PENDING_VERIFICATION.
 */
public record RegistrationDto(Long companyId, Long employeeId, String email, String employeeCode,
                              String status) {}
