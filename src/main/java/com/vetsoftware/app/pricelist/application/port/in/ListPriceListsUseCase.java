package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Listado sin filtro de empresa. Cumple
 * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} por la unica via que la regla
 * admite: {@code hasRole('SYSTEM')} a secas, sin disyuncion con ningun permiso
 * de empleado.
 */
public interface ListPriceListsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PriceListDto> listAll(int page, int pageSize);

    /**
     * Las tarifas en un estado concreto. Existe porque el unico subconjunto que un
     * cliente puede ofrecer para elegir es el de las PUBLISHED, y sin este filtro
     * la unica forma de conseguirlo era pedir el tope de 200 filas y descartar en
     * el navegador (incidencia #450).
     *
     * <p>
     * Eso no es solo caro: el dia que {@code price_lists} pase de 200 filas, una
     * tarifa publicada que caiga fuera de la primera pagina <strong>desaparece del
     * desplegable sin ningun error</strong>, y con ella se cotiza mal durante
     * semanas antes de que nadie lo note.
     *
     * <p>
     * Sigue sin filtrar por empresa -la tarifa es global y la tabla no tiene
     * {@code company_id}-, asi que el gate sigue siendo {@code hasRole('SYSTEM')} a
     * secas: acotar por estado no es acotar por tenant
     * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PriceListDto> listByStatus(PriceListStatus status, int page, int pageSize);
}
