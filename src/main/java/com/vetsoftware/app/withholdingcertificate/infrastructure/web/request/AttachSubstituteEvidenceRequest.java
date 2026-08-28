package com.vetsoftware.app.withholdingcertificate.infrastructure.web.request;

import com.vetsoftware.app.withholdingcertificate.domain.SubstituteEvidenceKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El sustituto que la ley admite cuando el cliente no expide el certificado.
 *
 * <p>
 * {@code evidenceKind} se pide explicitamente aunque hoy solo admita un valor:
 * el cuerpo dice <em>que</em> soporte se esta usando, y el dia que la norma
 * admita otro, las peticiones viejas siguen siendo legibles. Un cuerpo con solo
 * la referencia obligaria a adivinarlo.
 */
public record AttachSubstituteEvidenceRequest(
        @NotNull(message = "Debes indicar que soporte sustituye al certificado.") SubstituteEvidenceKind evidenceKind,
        @NotBlank(message = "Debes indicar la referencia del soporte sustituto.") @Size(max = 255, message = "La referencia del soporte no puede superar los 255 caracteres.") String evidenceRef) {
}
