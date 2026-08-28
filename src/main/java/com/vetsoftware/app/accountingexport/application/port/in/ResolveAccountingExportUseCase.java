package com.vetsoftware.app.accountingexport.application.port.in;

import com.vetsoftware.app.accountingexport.application.command.RejectAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ResolveAccountingExportUseCase {

    /**
     * El contador recibio el fichero.
     *
     * <p>
     * <strong>Se niega si el fichero ya tenia desenlace</strong>, y esa negativa es
     * toda la barandilla que hay: {@code chk_accounting_exports_lifecycle} mira la
     * fila, no de donde venia, asi que la transicion equivocada produce una fila
     * que el motor acepta sin una queja.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingExportDto markDelivered(Long id);

    /**
     * El contador devolvio el fichero, con su motivo. Es lo que libera el hueco de
     * {@code uq_accounting_exports_current} para el siguiente intento.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingExportDto markRejected(RejectAccountingExportCommand command);

    /**
     * El fichero queda reemplazado por un intento posterior. Puede llegar desde
     * cualquier estado —es la unica rama de
     * {@code chk_accounting_exports_lifecycle} sin condiciones— y borra la fecha de
     * entrega si la habia: dejarla seria afirmar que se entrego algo que se
     * sustituyo.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingExportDto markSuperseded(Long id);
}
