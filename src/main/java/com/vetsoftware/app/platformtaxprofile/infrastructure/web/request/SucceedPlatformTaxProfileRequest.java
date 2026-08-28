package com.vetsoftware.app.platformtaxprofile.infrastructure.web.request;

import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cambio de identidad fiscal. <strong>No es una actualizacion</strong>: cierra
 * la vigente y abre una nueva con estos datos, en una sola transaccion.
 *
 * <p>
 * <strong>Trae la ficha entera y no solo lo que cambia, y eso es
 * deliberado.</strong> Un cuerpo parcial obligaria a copiar de la ficha vieja
 * los campos que no vienen, y ese arrastre silencioso es como se acaba
 * imprimiendo la razon social nueva con el NIT de la anterior.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>: la tabla no tiene esa columna.
 *
 * @param effectiveFrom
 *            desde cuando rige la nueva, y tambien la fecha con la que se
 *            cierra la anterior — el intervalo es semiabierto. Tiene que ser
 *            <strong>estrictamente posterior</strong> al {@code validFrom} de
 *            la vigente: una identidad abierta hoy no se puede suceder hoy, y
 *            el servidor rechaza en vez de correr la fecha, porque de ella
 *            depende que razon social se imprime en una factura emitida en el
 *            intervalo
 */
public record SucceedPlatformTaxProfileRequest(
        @NotNull(message = "Debes indicar el tipo de documento.") PlatformDocumentType documentType,
        @NotBlank(message = "El numero de documento es obligatorio.") @Size(max = 20, message = "El numero de documento no puede superar los 20 caracteres.") String documentId,
        @Pattern(regexp = "\\d", message = "El digito de verificacion es una sola cifra.") String verificationDigit,
        @NotBlank(message = "La razon social es obligatoria.") @Size(max = 255, message = "La razon social no puede superar los 255 caracteres.") String legalName,
        @NotNull(message = "Debes indicar el regimen de IVA.") PlatformTaxRegime taxRegime,
        @NotBlank(message = "El correo fiscal es obligatorio.") @Size(max = 255, message = "El correo fiscal no puede superar los 255 caracteres.") String fiscalEmail,
        @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres.") String commercialName,
        @Schema(description = "Opcional: la columna es nulable.") Long economicActivityId,
        @jakarta.validation.constraints.NotNull(message = "Debes declarar si VetSoftware es autorretenedor.") Boolean selfWithholder,
        @NotNull(message = "Debes indicar desde cuando rige la identidad fiscal nueva.") @Schema(description = "Estrictamente posterior al validFrom de la vigente: una identidad abierta hoy no se puede suceder hoy.") LocalDate effectiveFrom) {
}
