package com.vetsoftware.app.gatewaysettlement.application.dto;

import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.SettlementAmounts;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La liquidacion tal como la consume la aplicacion.
 *
 * <p>
 * <strong>Los cinco importes salen planos.</strong> Anidar
 * {@code SettlementAmounts} dentro del DTO obligaria a publicar el value object
 * de dominio en el contrato de la API a traves de la Response, y el criterio
 * del repositorio es que la forma del JSON no la decida una invariante del
 * dominio.
 *
 * <p>
 * <strong>{@code totalCost} se deriva, no se guarda.</strong> Es
 * {@code fee + feeTax + gmf} —lo que cuesta cobrar, que es la pregunta que el
 * informe de margen hace— y una sexta columna se habria desincronizado de las
 * cinco a la primera correccion.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de version es la barandilla
 * del bloqueo optimista, no un dato del expediente. Publicarlo invitaria a un
 * cliente a mandarlo de vuelta y a construir un control de concurrencia
 * paralelo al que ya hace Hibernate.
 */
public record GatewaySettlementDto(Long id, String gateway, String settlementReference,
        String providerInvoiceRef, String providerTaxId, BigDecimal grossAmount,
        BigDecimal feeAmount, BigDecimal feeTaxAmount, BigDecimal gmfAmount, BigDecimal netAmount,
        BigDecimal totalCost, int paymentCount, LocalDate settledOn, Long bankReceiptId,
        LocalDateTime createdDate) {

    public static GatewaySettlementDto from(GatewaySettlement settlement) {
        SettlementAmounts amounts = settlement.getAmounts();
        return new GatewaySettlementDto(settlement.getId(), settlement.getGateway(),
                settlement.getSettlementReference(), settlement.getProviderInvoiceRef(),
                settlement.getProviderTaxId(), amounts.gross(), amounts.fee(), amounts.feeTax(),
                amounts.gmf(), amounts.net(), amounts.totalCost(), settlement.getPaymentCount(),
                settlement.getSettledOn(), settlement.getBankReceiptId(),
                settlement.getCreatedDate());
    }
}
