package com.vetsoftware.app.gatewaysettlement.application.port.in;

import com.vetsoftware.app.gatewaysettlement.application.command.AttachProviderInvoiceCommand;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AttachProviderInvoiceUseCase {

    /**
     * Escribe el soporte del gasto —factura del proveedor y su NIT— sobre un lote
     * ya cargado.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> El command recibe un
     * {@code id} y ningun {@code companyId}, que es exactamente el supuesto de
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}. Aqui no hay una empresa
     * con la que acotar la fila y tampoco la habria: el gasto es de Lumbre frente a
     * su pasarela, no de ninguna clinica.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    GatewaySettlementDto execute(AttachProviderInvoiceCommand command);
}
