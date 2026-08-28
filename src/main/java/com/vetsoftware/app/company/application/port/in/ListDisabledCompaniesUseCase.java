package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * El archivo de empresas: las que {@link ListCompaniesUseCase} <b>no puede
 * devolver</b>, paginado y con su mismo gate.
 *
 * <p>
 * <b>Por que hace falta un puerto aparte y no un flag en el listado.</b>
 * {@code CompanyJpaEntity} lleva {@code @SQLRestriction("enabled = true")}, asi
 * que una empresa archivada es invisible para TODA consulta JPA de la rodaja:
 * {@code GET /companies}, {@code GET /companies/search} y
 * {@code GET /companies/{id}} no pueden devolverla por mucho parametro que se
 * les añada, porque la restriccion se inyecta en el {@code WHERE} por debajo de
 * la consulta. Mientras no existio este puerto,
 * {@code PATCH /companies/{id}/enable} era un endpoint <b>inalcanzable desde la
 * consola</b>: restaurar una empresa exigia saberse el id de memoria, y en la
 * practica un {@code UPDATE} a mano en produccion.
 *
 * <p>
 * <b>El gate es el de {@link ListCompaniesUseCase}, literalmente el mismo.</b>
 * En la practica esto es una operacion de plataforma —quien restaura empresas
 * es la consola SYSTEM—, pero el alcance se declara igual que en el listado
 * activo y por la misma razon: {@code company.read} es un permiso que tiene
 * cualquier empleado con acceso a la ficha de su propia veterinaria, y sin
 * {@code isMyCompany} encima serviria el archivo mercantil completo —nombre,
 * NIT, direccion y telefono de todos los tenants dados de baja— a cualquiera de
 * ellos. El permiso dice <em>que</em> puede hacer el empleado, nunca <em>sobre
 * que filas</em>.
 *
 * <p>
 * El {@code companyId} no viene del cliente: lo pone el controller desde
 * {@code authz.currentCompanyIdOrNull()} —la empresa del empleado, o
 * {@code null} para un principal de plataforma, que es la unica señal de «sin
 * acotar»— y {@code @authz.isMyCompany} lo revalida aqui, de modo que una
 * llamada fabricada a mano tampoco pasa.
 *
 * <p>
 * <b>Y pagina por lo mismo que el listado activo</b> (VUE-06): el alcance
 * decide <em>que</em> filas, nunca <em>cuantas</em>. El tope duro es
 * {@code Pages.MAX_SIZE}, no el cliente.
 */
public interface ListDisabledCompaniesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.read') and @authz.isMyCompany(#companyId))")
    PageResult<CompanyDto> listDisabled(Long companyId, int page, int pageSize);
}
