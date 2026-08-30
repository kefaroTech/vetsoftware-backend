package com.vetsoftware.app.aiproposal.infrastructure.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El correo del titular que pide la supresion.
 *
 * <p>
 * &#9940; <strong>Va en el cuerpo y no en la ruta ni en la cadena de
 * consulta.</strong> {@code RequestLoggingContextFilter} mete
 * {@code getRequestURI()} en el contexto de log de toda peticion: un correo en
 * la URL acabaria en CloudWatch y en Loki con 31 dias de retencion, es decir
 * justo el dato que este endpoint existe para borrar, copiado a un sitio del
 * que no se borra.
 */
public record SuppressProposalDataRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Correo del titular cuyos datos se suprimen") @NotBlank @Email @Size(max = 320) String contactEmail) {
}
