package com.vetsoftware.app.platformaccess.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del formulario publico de solicitud de acceso.
 *
 * <p>
 * <b>No lleva ni puede llevar {@code companyId}</b>: el flujo es global de
 * plataforma. Tampoco lleva rol, permiso ni bandera alguna: lo que este flujo
 * termina creando tiene control total, y nada de eso puede venir del cliente.
 *
 * <p>
 * Los topes son los que el front ya valida, y se repiten en el dominio. Aqui
 * producen el error por campo que el front sabe pintar bajo cada input; alli
 * impiden que un camino que no pase por este controller escriba un motivo de
 * 5.000 caracteres —Resend corta cada variable a 2.000 y el correo saldria roto
 * o no saldria—.
 */
public record CreateAccessRequestRequest(
        @NotBlank(message = "El nombre completo es obligatorio.") @Size(min = 3, max = 120, message = "El nombre completo debe tener entre 3 y 120 caracteres.") String fullName,
        @NotBlank(message = "El correo electronico es obligatorio.") @Email(message = "El correo electronico no tiene un formato valido.") @Size(max = 150, message = "El correo electronico no puede superar los 150 caracteres.") String email,
        @NotBlank(message = "El motivo es obligatorio.") @Size(min = 20, max = 500, message = "El motivo debe tener entre 20 y 500 caracteres.") String reason) {
}
