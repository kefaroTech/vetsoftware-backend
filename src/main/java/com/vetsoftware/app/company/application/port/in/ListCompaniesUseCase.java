package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Listado de empresas <b>acotado por el alcance del actor</b>.
 *
 * <p>
 * El {@code companyId} no viene del cliente: lo pone el controller desde el
 * principal ({@code authz.currentCompanyIdOrNull()}). Para un empleado es
 * siempre su propia empresa —y {@code @authz.isMyCompany} lo revalida aquí, de
 * modo que un command fabricado a mano tampoco pasa—; para un principal de
 * plataforma (SYSTEM) es {@code null}, que es la señal de «sin acotar» y el
 * único camino al registro completo.
 *
 * <p>
 * <b>Por qué no basta {@code hasAuthority('company.read')} a secas.</b> Esa era
 * la puerta del defecto: {@code company.read} es un permiso que tiene cualquier
 * empleado con acceso a la ficha de su propia veterinaria, y con él
 * {@code GET /companies} devolvía el registro mercantil entero —nombre, NIT,
 * dirección, teléfono y plan contratado de todos los tenants— a cualquiera de
 * ellos. El permiso dice <em>qué</em> puede hacer el empleado, nunca <em>sobre
 * qué filas</em>.
 */
public interface ListCompaniesUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.read') and @authz.isMyCompany(#companyId))")
    List<CompanyDto> listAll(Long companyId);
}
