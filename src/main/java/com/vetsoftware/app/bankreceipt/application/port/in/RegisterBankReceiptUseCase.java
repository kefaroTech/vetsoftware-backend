package com.vetsoftware.app.bankreceipt.application.port.in;

import com.vetsoftware.app.bankreceipt.application.command.RegisterBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterBankReceiptUseCase {

    /**
     * Carga una linea del extracto bancario.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision, no un olvido.</strong> El extracto es el cuadre
     * interno de Lumbre contra su propio banco. Una entrada sin identificar no
     * tiene todavia dueño —esa es literalmente su definicion—, asi que no hay
     * ninguna clinica a la que pudiera pertenecer y no existe un {@code companyId}
     * con el que acotarla.
     *
     * <p>
     * <strong>Este parrafo existe para el dia que llegue la peticion.</strong>
     * Alguien pedira que el cliente vea «sus» consignaciones para reclamar una que
     * no se le abono. Quien atienda esa peticion no leera el changelog, leera este
     * puerto: abrir el camino de tenant no es sembrar un permiso, es tener que
     * decidir antes que ve una clinica de una bandeja donde por definicion todavia
     * no se sabe de quien es cada linea. Enseñarle la bandeja entera es enseñarle
     * los ingresos de sus competidoras.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    BankReceiptDto execute(RegisterBankReceiptCommand command);
}
