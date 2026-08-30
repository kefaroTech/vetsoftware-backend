package com.vetsoftware.app.aiproposal.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * La edicion manual del carrito.
 *
 * <p>
 * <strong>{@code version} es el bloqueo optimista, y no es opcional por
 * comodidad.</strong> Dos pestanas son dos clientes sobre la misma propuesta
 * sin sesion que los serialice; sin la version, un refinamiento en vuelo pisa
 * esta edicion y devuelve la linea que el usuario acababa de quitar.
 */
public record EditProposalLinesRequest(@NotBlank @Size(min = 43, max = 43) String token,
        List<@NotBlank @Size(max = 50) String> addedCodes,
        List<@NotBlank @Size(max = 50) String> removedCodes, Long version) {
}
