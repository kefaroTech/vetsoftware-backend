package com.vetsoftware.app.gatewaysettlement.infrastructure.web.response;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La liquidacion tal como sale por HTTP. <strong>Solo la ve la consola de
 * plataforma</strong>: no hay camino de tenant en esta feature, y no puede
 * haberlo mientras una fila agrupe los cobros de sesenta empresas.
 *
 * <p>
 * Los cuatro campos que van sin {@code REQUIRED} lo hacen a proposito: las dos
 * referencias del proveedor son nulas hasta que llega su factura y
 * {@code bankReceiptId} lo es hasta que el lote se ata al extracto. Marcarlos
 * obligatorios haria que el tipo generado para el front prometiera un valor que
 * la mayoria de las filas recien cargadas no tiene.
 */
public record GatewaySettlementResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String gateway,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String settlementReference,
        String providerInvoiceRef, String providerTaxId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal grossAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal feeAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal feeTaxAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal gmfAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal netAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Comision + impuesto de la comision + gravamen: lo que costo"
                + " cobrar. Derivado, no almacenado.") BigDecimal totalCost,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int paymentCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate settledOn,
        Long bankReceiptId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdDate) {

    public static GatewaySettlementResponse from(GatewaySettlementDto dto) {
        return new GatewaySettlementResponse(dto.id(), dto.gateway(), dto.settlementReference(),
                dto.providerInvoiceRef(), dto.providerTaxId(), dto.grossAmount(), dto.feeAmount(),
                dto.feeTaxAmount(), dto.gmfAmount(), dto.netAmount(), dto.totalCost(),
                dto.paymentCount(), dto.settledOn(), dto.bankReceiptId(), dto.createdDate());
    }
}
