package com.vetsoftware.app.gatewaysettlement.application.port.in;

import com.vetsoftware.app.gatewaysettlement.application.command.RegisterGatewaySettlementCommand;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterGatewaySettlementUseCase {

    /**
     * Carga una liquidacion de la pasarela.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es LA decision de esta feature, no un olvido.</strong> Una fila de
     * {@code gateway_settlements} agrupa los cobros de muchas clinicas: sesenta
     * cobros de sesenta empresas en un solo abono, con un unico bruto y una unica
     * comision. No existe el {@code companyId} con el que acotarla porque el lote
     * no es de nadie en particular.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que llegue la peticion.</strong>
     * Alguien pedira que el cliente vea «su» liquidacion, porque el detalle de su
     * pago ya le enseña la referencia del lote y el siguiente paso natural es
     * hacerla pinchable. Quien atienda esa peticion no leera el changelog, leera
     * este puerto: <b>abrir el lote desde la referencia que ve el cliente le enseña
     * los importes de las otras cincuenta y nueve empresas</b>. Lo que si puede
     * abrirse es lo ya conciliado y atribuido a ESE cobro, que es otro recurso y
     * vive en la rodaja del pago.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    GatewaySettlementDto execute(RegisterGatewaySettlementCommand command);
}
