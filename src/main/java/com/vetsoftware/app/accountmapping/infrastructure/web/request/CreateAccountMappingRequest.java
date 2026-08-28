package com.vetsoftware.app.accountmapping.infrastructure.web.request;

import com.vetsoftware.app.accountmapping.domain.MappingKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId} por ninguna via</strong>: el puente concepto →
 * cuenta no tiene empresa a la que apuntar.
 *
 * @param mappingKey
 *            la subclave dentro de la clase. <b>Nunca vacia</b>: donde no hay
 *            subclave se escribe {@code '-'}, porque una columna nulable dentro
 *            de una unicidad no restringe nada
 * @param catalogItemId
 *            solo para {@code REVENUE} y {@code DEFERRED_REVENUE}, igual que
 *            {@code chargeType} y {@code taxTreatment}. El «solo» lo valida el
 *            dominio, que es donde vive porque es una regla entre dos campos
 * @param deferredAccountCode
 *            solo para esas mismas dos clases
 */
public record CreateAccountMappingRequest(
        @NotNull(message = "Debes indicar la clase del mapeo.") MappingKind mappingKind,
        @NotBlank(message = "Debes indicar la subclave del mapeo; usa '-' si no aplica.") @Size(max = 60, message = "La subclave no puede superar los 60 caracteres.") String mappingKey,
        @Positive(message = "El identificador del articulo debe ser positivo.") Long catalogItemId,
        @Size(max = 20, message = "El tipo de cargo no puede superar los 20 caracteres.") String chargeType,
        @Size(max = 20, message = "El tratamiento fiscal no puede superar los 20 caracteres.") String taxTreatment,
        @NotBlank(message = "Debes indicar la cuenta debito.") @Size(max = 10, message = "El codigo de la cuenta debito no puede superar los 10 caracteres.") @Schema(description = "Codigo de una cuenta que admita asiento (nivel 6).") String debitAccountCode,
        @NotBlank(message = "Debes indicar la cuenta credito.") @Size(max = 10, message = "El codigo de la cuenta credito no puede superar los 10 caracteres.") @Schema(description = "Codigo de una cuenta que admita asiento (nivel 6).") String creditAccountCode,
        @Size(max = 10, message = "El codigo de la cuenta de diferido no puede superar los 10 caracteres.") String deferredAccountCode,
        @NotNull(message = "Debes indicar desde cuando aplica el mapeo.") LocalDate validFrom,
        @Schema(description = "Nulo abre la vigencia; con fecha el mapeo entra ya cerrado.") LocalDate validTo) {
}
