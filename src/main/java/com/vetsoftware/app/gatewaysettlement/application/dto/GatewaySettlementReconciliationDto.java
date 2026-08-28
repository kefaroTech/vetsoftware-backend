package com.vetsoftware.app.gatewaysettlement.application.dto;

import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.PaymentCountReconciliation;

/**
 * El resultado de contrastar lo que el lote declara contra los cobros que de
 * verdad cuelgan de el.
 *
 * <p>
 * <strong>Lleva la referencia del lote y NO los cobros.</strong> Enumerar aqui
 * los pagos enlazados seria enseñar, en una sola respuesta, que empresas
 * cobraron y cuanto — que es exactamente la fuga que esta rodaja evita. Lo que
 * el operario necesita para trabajar es el numero: si no cuadra, va a buscar el
 * pago perdido desde el lado del cobro, que si esta acotado por empresa.
 */
public record GatewaySettlementReconciliationDto(Long settlementId, String gateway,
        String settlementReference, int declaredPayments, long linkedPayments, long difference,
        boolean balanced) {

    public static GatewaySettlementReconciliationDto from(GatewaySettlement settlement,
            PaymentCountReconciliation reconciliation) {
        return new GatewaySettlementReconciliationDto(settlement.getId(), settlement.getGateway(),
                settlement.getSettlementReference(), reconciliation.declared(),
                reconciliation.linked(), reconciliation.difference(), reconciliation.isBalanced());
    }
}
