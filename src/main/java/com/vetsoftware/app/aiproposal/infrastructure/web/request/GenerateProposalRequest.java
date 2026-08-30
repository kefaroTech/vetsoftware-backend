package com.vetsoftware.app.aiproposal.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * La pantalla 1.
 *
 * <p>
 * &#9940; <strong>Sin {@code companyId} y sin token.</strong> No hay empresa a
 * la que pertenecer -un prospecto no es un cliente- y todavia no hay propuesta
 * que identificar: el token nace en la respuesta.
 *
 * <p>
 * <strong>El minimo son 15 caracteres, no 30.</strong> Es el unico de los tres
 * numeros que llegaron a circular que traia argumento escrito: "Clinica de
 * barrio, consulta general y vacunas" son 38 caracteres y bastan para proponer
 * historia clinica, vacunacion y agenda, asi que un umbral alto castiga a quien
 * escribe bien y corto -que es el usuario con prisa que este producto tiene-.
 * El suelo esta en 15 porque "hola" no es informacion, y el texto corto pero
 * pobre lo resuelve el modelo con {@code understood = false}, que es una
 * pantalla util y no un 400.
 *
 * <p>
 * &#9888; Las restricciones solo se evaluan con {@code @Valid} delante del
 * {@code @RequestBody}: sin el, el binder no dispara el validador y lo que se
 * lee perfecto en el diff no lo comprueba nadie. Lo vigila
 * {@code CUERPO_CON_RESTRICCIONES_SE_VALIDA}.
 */
public record GenerateProposalRequest(@NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 15, max = 1000) String description,
        @NotEmpty @Valid List<LegalAcceptanceRequest> acceptances) {
}
