package com.vetsoftware.app.bankreceipt.application.port.in;

import com.vetsoftware.app.bankreceipt.application.command.DiscardBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DiscardBankReceiptUseCase {

    /**
     * Archiva la entrada sin borrarla: pasa a {@code DISCARDED} y sella la misma
     * columna que una identificada.
     *
     * <p>
     * <strong>No es un borrado logico y no debe confundirse con uno.</strong> Un
     * {@code enabled = false} esconde la fila de todas las consultas; esto la deja
     * a la vista y solo la saca de la bandeja de pendientes. La diferencia importa
     * el dia que el banco reclame una linea que aqui se dio por no valida.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas</strong>, por lo mismo que el resto
     * del slice.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    BankReceiptDto execute(DiscardBankReceiptCommand command);
}
