package com.vetsoftware.app.withholdingraterule.application.port.in;

import com.vetsoftware.app.withholdingraterule.application.command.CloseWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CloseWithholdingRateRuleUseCase {

    /**
     * Pone fecha de fin a una vigencia abierta. Es el paso obligado antes de
     * publicar su relevo: mientras la regla siga abierta,
     * {@code uq_withholding_rate_rules_current} impide crear otra para el mismo
     * supuesto.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Senala una fila concreta
     * por su id y no recibe empresa —no la hay, la tabla es un catalogo global—,
     * que es exactamente el caso que
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM} exige cerrar a plataforma.
     * Y ademas es escritura, que en este bloque nunca es del tenant.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    WithholdingRateRuleDto execute(CloseWithholdingRateRuleCommand command);
}
