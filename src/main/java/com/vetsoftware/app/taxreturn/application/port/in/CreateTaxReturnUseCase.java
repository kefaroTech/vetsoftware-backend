package com.vetsoftware.app.taxreturn.application.port.in;

import com.vetsoftware.app.taxreturn.application.command.CreateTaxReturnCommand;
import com.vetsoftware.app.taxreturn.application.dto.TaxReturnDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateTaxReturnUseCase {

    /**
     * Abre el borrador de una declaracion inicial.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Son las declaraciones de
     * Lumbre ante la DIAN: cero superficie de cliente, ninguna empresa a la que
     * acotar y ningun permiso de tenant que deba alcanzarlas. Toda la feature
     * comparte ese gate ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    TaxReturnDto execute(CreateTaxReturnCommand command);
}
