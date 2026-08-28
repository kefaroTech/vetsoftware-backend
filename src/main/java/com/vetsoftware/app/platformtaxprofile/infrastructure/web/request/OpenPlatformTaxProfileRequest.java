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
 * Alta de la primera identidad fiscal de VetSoftware.
 *
 * <p>
 * <strong>Sin {@code companyId} por ninguna via, y aqui no hace falta ni
 * siquiera como {@code @RequestParam}.</strong> En las rutas de plataforma de
 * otros bloques tesoreria elige a que clinica le afecta, asi que la empresa
 * viaja por la query string —{@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe el
 * cuerpo y solo el cuerpo—. Esta tabla no tiene empresa a la que apuntar: es la
 * identidad de la plataforma, una sola para todos.
 *
 * <p>
 * <strong>Lo que se escriba aqui acaba impreso en la factura de cada
 * cliente.</strong> Por eso el changeset 367 dejo la tabla sin sembrar en vez
 * de inventarse los datos, y por eso este request no tiene ningun valor por
 * defecto: todo lo obligatorio se declara.
 *
 * @param verificationDigit
 *            digito de verificacion del NIT, una sola cifra. Obligatorio de
 *            hecho solo para {@code NIT} y prohibido para los demas: ese «si y
 *            solo si» mira dos campos, asi que lo valida el dominio y no una
 *            anotacion
 * @param economicActivityId
 *            <strong>opcional</strong>: la columna {@code economic_activity_id}
 *            es nulable. Si viene, tiene que existir y estar habilitada en
 *            {@code economic_activities}
 * @param selfWithholder
 *            si <strong>VetSoftware</strong> es autorretenedor. No se deduce de
 *            que sus clientes le retengan: los dos hechos coexisten
 * @param validFrom
 *            desde cuando rige. Explicito y no del reloj, porque una identidad
 *            puede registrarse hoy para regir desde el primero del mes que
 *            viene
 */
public record OpenPlatformTaxProfileRequest(
        @NotNull(message = "Debes indicar el tipo de documento.") PlatformDocumentType documentType,
        @NotBlank(message = "El numero de documento es obligatorio.") @Size(max = 20, message = "El numero de documento no puede superar los 20 caracteres.") String documentId,
        @Pattern(regexp = "\\d", message = "El digito de verificacion es una sola cifra.") @Schema(description = "Solo el NIT lo lleva. Sale del modulo 11 y se copia del RUT: no se calcula aqui.") String verificationDigit,
        @NotBlank(message = "La razon social es obligatoria.") @Size(max = 255, message = "La razon social no puede superar los 255 caracteres.") @Schema(description = "Se imprime en la factura de cada cliente.") String legalName,
        @NotNull(message = "Debes indicar el regimen de IVA.") PlatformTaxRegime taxRegime,
        @NotBlank(message = "El correo fiscal es obligatorio.") @Size(max = 255, message = "El correo fiscal no puede superar los 255 caracteres.") String fiscalEmail,
        @Size(max = 150, message = "El nombre comercial no puede superar los 150 caracteres.") String commercialName,
        @Schema(description = "Opcional: la columna es nulable.") Long economicActivityId,
        @Schema(description = "Si VetSoftware es autorretenedor. Coexiste con que sus clientes le retengan; no se deduce lo uno de lo otro.") @jakarta.validation.constraints.NotNull(message = "Debes declarar si VetSoftware es autorretenedor.") Boolean selfWithholder,
        @NotNull(message = "Debes indicar desde cuando rige la identidad fiscal.") LocalDate validFrom) {
}
