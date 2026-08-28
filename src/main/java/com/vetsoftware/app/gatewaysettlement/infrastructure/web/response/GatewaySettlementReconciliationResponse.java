package com.vetsoftware.app.gatewaysettlement.infrastructure.web.response;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementReconciliationDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * El contraste entre los cobros que el lote declara y los que de verdad cuelgan
 * de el.
 *
 * <p>
 * <strong>Devuelve numeros, nunca la lista de cobros.</strong> Enumerarlos
 * seria enseñar en una sola respuesta que empresas cobraron y cuanto — la fuga
 * que esta rodaja existe para evitar. Con la diferencia en la mano, la busqueda
 * del pago perdido se hace desde el lado del cobro, que si esta acotado por
 * empresa.
 */
public record GatewaySettlementReconciliationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long settlementId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String gateway,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String settlementReference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cobros que el lote dice traer.") int declaredPayments,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Cobros realmente atados al lote.") long linkedPayments,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Declarados menos enlazados. Positivo: falta atar un cobro."
                + " Negativo: hay uno atado de mas.") long difference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean balanced) {

    public static GatewaySettlementReconciliationResponse from(
            GatewaySettlementReconciliationDto dto) {
        return new GatewaySettlementReconciliationResponse(dto.settlementId(), dto.gateway(),
                dto.settlementReference(), dto.declaredPayments(), dto.linkedPayments(),
                dto.difference(), dto.balanced());
    }
}
