package com.vetsoftware.app.companybillingprofile.application.port.in;

import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Una ficha concreta del historico, por su id.
 *
 * <p>
 * <strong>Existe porque una factura apunta a una ficha por id.</strong>
 * {@code subscription_billing_documents} lleva {@code billing_profile_id} y su
 * clave foranea es compuesta —{@code (company_id, billing_profile_id)}—, que es
 * la misma acotacion que hace este puerto: quien abre una factura de hace un
 * año necesita ver la ficha con la que se emitio, y esa ya no es la vigente.
 */
public interface FindCompanyBillingProfileUseCase {

    /**
     * <strong>El {@code companyId} es obligatorio y no es decorativo.</strong> El
     * {@code id} lo escribe el cliente en la URL, asi que sin el segundo parametro
     * cualquier empleado autenticado podria pedir la ficha numero 1 y leer el NIT,
     * la direccion y el correo de facturacion de otra clinica. Es exactamente el
     * hueco que cierra {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}: el
     * permiso dice <em>que</em> puede hacer un empleado, nunca <em>sobre que
     * filas</em>.
     *
     * <p>
     * Y el filtro va en la consulta, no en un {@code if} despues de cargarla: el
     * puerto de salida solo ofrece {@code findByIdAndCompanyId}, asi que no existe
     * la variante ancha que alguien pudiera llamar por descuido.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.read') and"
            + " @authz.isMyCompany(#companyId))")
    CompanyBillingProfileDto findById(Long id, Long companyId);
}
