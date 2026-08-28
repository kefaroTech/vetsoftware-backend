package com.vetsoftware.app.withholdingcertificate.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Sin {@code companyId} ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}) y sin
 * {@code id}: el certificado se senala en la ruta y el controller lo pone en el
 * command. Un id en el cuerpo y otro en la URL son dos fuentes de verdad para
 * el mismo dato, y la discrepancia no falla, escribe en la fila equivocada.
 *
 * @param fileRef
 *            referencia del archivo guardado. Obligatoria aqui y en el dominio:
 *            un certificado recibido sin archivo es un certificado que nadie
 *            puede ensenar ante la administracion
 */
public record ReceiveWithholdingCertificateRequest(
        @NotNull(message = "Debes indicar cuando llego el certificado.") LocalDate receivedOn,
        @NotBlank(message = "Debes indicar el archivo del certificado recibido.") @Size(max = 255, message = "La referencia del archivo no puede superar los 255 caracteres.") String fileRef) {
}
