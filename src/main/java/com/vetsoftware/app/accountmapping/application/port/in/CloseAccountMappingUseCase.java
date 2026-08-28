package com.vetsoftware.app.accountmapping.application.port.in;

import com.vetsoftware.app.accountmapping.application.command.CloseAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CloseAccountMappingUseCase {

    /**
     * Pone fecha de fin a un mapeo vigente. Es el paso obligado antes de publicar
     * su relevo: mientras el mapeo siga abierto,
     * {@code uq_account_mappings_current} impide crear otro para el mismo supuesto.
     *
     * <p>
     * <strong>No hay {@code Update}.</strong> Editar la cuenta de un mapeo en sitio
     * reescribiria en silencio contra que cuenta se asentaron todas las facturas
     * anteriores.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountMappingDto execute(CloseAccountMappingCommand command);
}
