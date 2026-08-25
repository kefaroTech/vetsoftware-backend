package com.vetsoftware.app.platformaccess.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de aprobar y de rechazar. Es el mismo para los dos: el mismo codigo
 * sirve para ambas decisiones, no hay un codigo por decision.
 *
 * <p>
 * El token viaja en el cuerpo y no en la query porque estos son POST. En los
 * dos GET de validacion si va por query, que es lo que el front ya construyo,
 * con la consecuencia asumida de que ahi acaba en el historial del navegador y
 * en los logs de acceso del balanceador —nada de eso lo controla este
 * repositorio, y lo que acota el dano es que el token sea de un solo uso y de
 * vida corta—.
 */
public record ResolveAccessRequestRequest(
        @NotBlank(message = "El token es obligatorio.") @Size(max = 200, message = "El token no tiene un formato valido.") String token,
        @NotBlank(message = "El codigo de verificacion es obligatorio.") @Pattern(regexp = "\\d{6}", message = "El codigo de verificacion debe tener exactamente 6 digitos.") String code) {
}
