package com.vetsoftware.app.withholdingraterule.infrastructure.web.response;

import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import com.vetsoftware.app.withholdingraterule.domain.ServiceNature;
import com.vetsoftware.app.withholdingraterule.domain.WithholdingType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La tarifa de retencion tal como sale por HTTP. La ven tanto la consola de
 * plataforma como el cliente: el bloque «Referencia fiscal y textos» del
 * documento maestro lo reparte como <em>escribe plataforma, leen ambos</em>, y
 * saber que retencion esperar es lo que permite al cliente cuadrar su giro.
 *
 * <p>
 * <strong>{@code ratePercent} sale con seis decimales y sin redondear.</strong>
 * Es el campo que un front puede estropear solo con formatearlo: mostrar
 * {@code 0.69} en vez de {@code 0.690000} es cosmetica, pero <em>enviar</em> de
 * vuelta el valor truncado, o calcular sobre el, retiene de menos en cada
 * factura sin dar un error.
 *
 * <p>
 * <strong>No lleva {@code version}</strong> —es una barandilla del que escribe,
 * no un dato de la tarifa— ni las dos columnas generadas
 * ({@code municipality_key}, {@code current_rule_marker}): son detalle del
 * motor, existen para que dos indices unicos puedan restringir lo que con
 * {@code NULL} no restringian, y publicarlas invitaria a construir logica sobre
 * un centinela de base de datos.
 */
public record WithholdingRateRuleResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) WithholdingType withholdingType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ServiceNature serviceNature,
        @Schema(description = "Codigo DIVIPOLA. Presente solo en las reglas de ICA.") String municipalityCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Porcentaje, no fraccion: el 6,9 por mil es 0.690000.") BigDecimal ratePercent,
        BigDecimal minimumBaseAmount, BigDecimal minimumBaseUvt, String legalReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate validFrom,
        @Schema(description = "Nulo mientras la vigencia siga abierta.") LocalDate validTo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean enabled) {

    public static WithholdingRateRuleResponse from(WithholdingRateRuleDto dto) {
        return new WithholdingRateRuleResponse(dto.id(), dto.withholdingType(), dto.serviceNature(),
                dto.municipalityCode(), dto.ratePercent(), dto.minimumBaseAmount(),
                dto.minimumBaseUvt(), dto.legalReference(), dto.validFrom(), dto.validTo(),
                dto.createdDate(), dto.enabled());
    }
}
