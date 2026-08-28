package com.vetsoftware.app.accountmapping.application.port.in;

import com.vetsoftware.app.accountmapping.application.command.CreateAccountMappingCommand;
import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateAccountMappingUseCase {

    /**
     * Publica un mapeo concepto → cuenta.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Decidir contra que cuenta
     * se asienta cada concepto es una decision del contador de VetSoftware sobre
     * sus propios libros; no hay empresa a la que acotar ni permiso de tenant que
     * pueda alcanzarla. Toda la feature comparte ese gate, que es lo que exige
     * {@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AccountMappingDto execute(CreateAccountMappingCommand command);
}
