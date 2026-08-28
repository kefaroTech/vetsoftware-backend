package com.vetsoftware.app.withholdingraterule.application.port.in;

import com.vetsoftware.app.withholdingraterule.application.command.CreateWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.dto.WithholdingRateRuleDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateWithholdingRateRuleUseCase {

    /**
     * Da de alta una tarifa de retencion.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision.</strong> El bloque «Referencia fiscal y textos» del
     * documento maestro lo reparte como <em>escribe plataforma, leen ambos</em>: la
     * tarifa la fija la ley, no la clinica. Abrirlo por permiso dejaria que un
     * tenant escribiera su propia retencion esperada —y con ella, cuanto cree que
     * le van a girar— sobre un catalogo que leen todos los demas.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    WithholdingRateRuleDto execute(CreateWithholdingRateRuleCommand command);
}
