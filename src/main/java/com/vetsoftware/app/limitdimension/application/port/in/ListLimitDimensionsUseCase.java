package com.vetsoftware.app.limitdimension.application.port.in;

import com.vetsoftware.app.limitdimension.application.dto.LimitDimensionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El catálogo entero de ejes.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas, que es lo que exige
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29) a todo listado que no filtra
 * por empresa. Aquí no la filtra porque la tabla no la tiene.
 */
public interface ListLimitDimensionsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    List<LimitDimensionDto> listAll();
}
