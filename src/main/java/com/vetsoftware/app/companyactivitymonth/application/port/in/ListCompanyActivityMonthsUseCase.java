package com.vetsoftware.app.companyactivitymonth.application.port.in;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListCompanyActivityMonthsUseCase {

    /**
     * Toda la actividad de todas las clinicas, paginada.
     *
     * <p>
     * <strong>No filtra por empresa a proposito, y por eso solo lo puede servir
     * {@code hasRole('SYSTEM')} a secas.</strong> El repositorio <em>si</em> sabe
     * filtrar por empresa —declara {@link #listByCompany}—, asi que un
     * {@code find…} suyo que devuelva varias filas sin ese filtro devuelve filas de
     * todos los tenants: es literalmente el supuesto de
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} (BE-29, regla dura). Aqui el barrido
     * cruzado <b>es</b> el producto: comparar clinicas entre si es lo que convierte
     * la tabla en un informe.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CompanyActivityMonthDto> listAll(int page, int pageSize);

    /**
     * La serie de una sola clinica, mes a mes y de lo mas reciente a lo mas
     * antiguo.
     *
     * <p>
     * Es el hermano acotado que exige la convencion: lo que se necesite mirar de
     * una empresa concreta sale por aqui, no filtrando en memoria el listado ancho.
     * Sigue siendo {@code SYSTEM} —la serie no la lee el tenant—, pero la consulta
     * baja a la base con la empresa dentro y se apoya en {@code uq_cam_month}, que
     * ademas es el indice de {@code fk_cam_company}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CompanyActivityMonthDto> listByCompany(Long companyId, int page, int pageSize);

    /**
     * Todas las clinicas en un mes concreto: la foto transversal del periodo.
     *
     * <p>
     * Tampoco filtra por empresa, y por el mismo motivo esta cerrado a
     * {@code SYSTEM} a secas.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CompanyActivityMonthDto> listByPeriod(String periodKey, int page, int pageSize);
}
