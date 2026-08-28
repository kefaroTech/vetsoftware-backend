package com.vetsoftware.app.gatewaysettlement.application.port.in;

import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementReconciliationDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReconcileGatewaySettlementUseCase {

    /**
     * Contrasta los cobros que el lote declara contra los que de verdad cuelgan de
     * el: <b>si dice 37 y hay 36, hay un pago perdido</b>.
     *
     * <p>
     * Es la operacion por la que {@code payment_count} existe. Sin ella la columna
     * es un numero que nadie mira, y un cobro que la pasarela liquido pero que
     * nunca se ato a su lote no deja rastro: el dinero cuadra —entro en el bruto—
     * pero el cobro queda sin conciliar y la clinica aparece debiendo lo que ya
     * pago.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>: la respuesta habla del
     * lote entero, es decir de todas las empresas a la vez.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    GatewaySettlementReconciliationDto reconcile(Long id);
}
