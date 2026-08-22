package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Búsqueda de empresas por nombre o identificador fiscal, con el mismo alcance
 * y la misma página que el listado.
 *
 * <p>
 * Nace con el listado paginado y no antes por una razón concreta: una vez que
 * {@code GET /companies} deja de devolver el censo entero, encontrar una
 * empresa dejaba de ser posible sin recorrer las páginas a mano. Filtrar en
 * cliente ya no vale —el cliente solo tiene la página que está mirando—, así
 * que el filtro baja al servidor.
 *
 * <p>
 * <b>El gate es literalmente el de {@link ListCompaniesUseCase}, a
 * propósito.</b> Buscar es leer el mismo registro con un {@code WHERE} más: si
 * el alcance fuera más ancho aquí, la búsqueda sería el camino corto para leer
 * lo que el listado niega. El {@code companyId} lo sigue poniendo el servidor
 * desde el principal, y para un empleado el resultado es, como mucho, su propia
 * empresa.
 */
public interface SearchCompaniesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.read') and @authz.isMyCompany(#companyId))")
    PageResult<CompanyDto> search(Long companyId, String query, int page, int pageSize);
}
