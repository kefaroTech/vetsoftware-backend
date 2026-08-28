package com.vetsoftware.app.bankreceipt.application.port.in;

import com.vetsoftware.app.bankreceipt.application.command.IdentifyBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IdentifyBankReceiptUseCase {

    /**
     * Saca la entrada de la bandeja y sella la hora con el reloj del negocio.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, por lo mismo que el alta:
     * decidir que una consignacion ya esta explicada es un acto de tesoreria de la
     * plataforma. Si lo pudiera hacer el propio cliente, cualquiera podria vaciar
     * la bandeja apropiandose de un ingreso que no mando.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    BankReceiptDto execute(IdentifyBankReceiptCommand command);
}
