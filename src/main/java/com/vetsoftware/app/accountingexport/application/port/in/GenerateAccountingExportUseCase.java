package com.vetsoftware.app.accountingexport.application.port.in;

import com.vetsoftware.app.accountingexport.application.command.GenerateAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GenerateAccountingExportUseCase {

    /**
     * Registra un fichero de exportacion recien generado, con el numero de intento
     * que le toca.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Es un fichero de los
     * libros de Lumbre: no hay empresa a la que acotar y ningun permiso de tenant
     * debe poder alcanzarlo. Toda la feature comparte ese gate
     * ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountingExportDto execute(GenerateAccountingExportCommand command);
}
