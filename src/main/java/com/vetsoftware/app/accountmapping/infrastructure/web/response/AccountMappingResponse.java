package com.vetsoftware.app.accountmapping.infrastructure.web.response;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.domain.MappingKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * El mapeo tal como sale por HTTP. Solo lo ve la consola de plataforma.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> ni ninguna de las cuatro columnas
 * generadas ({@code catalog_item_key}, {@code charge_type_key},
 * {@code tax_treatment_key}, {@code current_mapping_marker}): son detalle del
 * motor, existen para que dos indices unicos puedan restringir lo que con
 * {@code NULL} no restringian, y publicarlas invitaria a construir logica sobre
 * un centinela de base de datos.
 */
public record AccountMappingResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) MappingKind mappingKind,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "La subclave dentro de la clase; '-' cuando no aplica.") String mappingKey,
        @Schema(description = "Presente solo en los mapeos de ingreso.") Long catalogItemId,
        String chargeType, String taxTreatment,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String debitAccountCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String creditAccountCode,
        @Schema(description = "Presente solo en los mapeos de ingreso.") String deferredAccountCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom,
        @Schema(description = "Nulo mientras la vigencia siga abierta.") LocalDate validTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {

    public static AccountMappingResponse from(AccountMappingDto dto) {
        return new AccountMappingResponse(dto.id(), dto.mappingKind(), dto.mappingKey(),
                dto.catalogItemId(), dto.chargeType(), dto.taxTreatment(), dto.debitAccountCode(),
                dto.creditAccountCode(), dto.deferredAccountCode(), dto.validFrom(), dto.validTo(),
                dto.createdDate(), dto.enabled());
    }
}
