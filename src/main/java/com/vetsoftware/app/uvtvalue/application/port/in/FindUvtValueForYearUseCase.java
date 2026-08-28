package com.vetsoftware.app.uvtvalue.application.port.in;

import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Resuelve la UVT <strong>del ano que se pide</strong>.
 *
 * <p>
 * No hay hermana que devuelva «la vigente» y esa ausencia es el diseno: la
 * unica forma de meter la cifra de este ano en el calculo de un ano viejo seria
 * que existiera un metodo que no preguntara por el ano.
 *
 * <p>
 * Recibe {@code companyId} aunque la tabla no lo tenga: leen los dos lados y es
 * lo que permite cerrar la via del empleado a su propia empresa en vez de
 * abrirla por una autoridad suelta.
 */
public interface FindUvtValueForYearUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('uvt.read') and @authz.isMyCompany(#companyId))")
    UvtValueDto findByYear(int fiscalYear, Long companyId);
}
