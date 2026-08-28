package com.vetsoftware.app.limitdimension.application.port.in;

import com.vetsoftware.app.limitdimension.application.command.UpdateLimitDimensionCommand;
import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambia lo editable de un eje: su nombre, el submódulo del que cuelga y los
 * días de enfriamiento tras borrar.
 *
 * <p>
 * Existía {@code LimitDimension.update(...)} en el dominio y no lo llamaba
 * nadie: el eje se podía declarar y consultar, pero corregirle una errata al
 * nombre exigía una migración. Este puerto es lo que cierra ese hueco.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas, por las dos razones a la
 * vez. El command lleva un {@code id} que el cliente escribe, que es lo que
 * vigila {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}; y la tabla no
 * tiene {@code company_id} —es catálogo global—, así que la salida correcta es
 * cerrar la operación a un principal cross-tenant y no pedir un
 * {@code companyId} que no existe.
 */
public interface UpdateLimitDimensionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    LimitDimensionDto execute(UpdateLimitDimensionCommand command);
}
