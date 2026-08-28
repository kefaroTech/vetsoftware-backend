package com.vetsoftware.app.limitdimension.application.port.in;

import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Un eje por su id.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. El id lo escribe el cliente,
 * que es exactamente lo que vigila
 * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}; como la fila no pertenece
 * a ninguna empresa, la salida correcta es cerrar la operación a un principal
 * cross-tenant, no pedir un {@code companyId} que no existe.
 */
public interface FindLimitDimensionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    LimitDimensionDto findById(Long id);
}
