package com.vetsoftware.app.limitdimension.application.port.in;

import com.vetsoftware.app.limitdimension.application.command.CreateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Declara un eje limitable. Es la operación que convierte «vender un límite
 * nuevo» en una fila y no en un despliegue.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. La tabla no tiene
 * {@code company_id} —es catálogo global— y con eso se satisface la familia de
 * reglas de tenancy, que no tendría empresa que validar.
 */
public interface CreateLimitDimensionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    LimitDimensionDto execute(CreateLimitDimensionCommand command);
}
