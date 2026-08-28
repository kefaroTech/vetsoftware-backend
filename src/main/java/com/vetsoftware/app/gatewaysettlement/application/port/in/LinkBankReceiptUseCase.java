package com.vetsoftware.app.gatewaysettlement.application.port.in;

import com.vetsoftware.app.gatewaysettlement.application.command.LinkBankReceiptCommand;
import com.vetsoftware.app.gatewaysettlement.application.dto.GatewaySettlementDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface LinkBankReceiptUseCase {

    /**
     * Ata el lote a la linea del extracto por la que entro su neto.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, y aqui hay una razon
     * adicional a las de sus hermanas: el otro extremo de la operacion es
     * {@code bank_receipts}, que tambien es cuadre interno de plataforma y esta
     * cerrado igual. Abrir este puerto por permiso dejaria a un empleado de una
     * clinica atar lotes ajenos a lineas de extracto ajenas.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    GatewaySettlementDto execute(LinkBankReceiptCommand command);
}
