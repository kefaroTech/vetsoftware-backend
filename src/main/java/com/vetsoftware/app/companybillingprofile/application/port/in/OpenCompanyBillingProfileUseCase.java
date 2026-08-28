package com.vetsoftware.app.companybillingprofile.application.port.in;

import com.vetsoftware.app.companybillingprofile.application.command.OpenCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Abre la primera ficha de facturacion de una empresa. */
public interface OpenCompanyBillingProfileUseCase {

    /**
     * <strong>{@code #command.companyId} tiene que escribirse exactamente
     * asi.</strong> El parametro del metodo se llama {@code command}: si alguien
     * copia aqui un {@code #companyId} de otro puerto, SpEL lo resuelve a
     * {@code null} sin decir nada, {@code isMyCompany(null)} devuelve {@code false}
     * y el endpoint queda cerrado a todo el mundo salvo SYSTEM. La regla falla
     * siempre, que en una anotacion de seguridad se nota tarde.
     *
     * <p>
     * <strong>La comprobacion es defensa en profundidad, no la unica.</strong> El
     * controller ya inyecta el {@code companyId} desde el principal —el cuerpo no
     * lo trae, ni podria—, asi que esto protege contra otro caller y contra un
     * cambio futuro que lo pase distinto.
     *
     * <p>
     * <strong>Sobre el permiso: {@code company.update} y no uno propio.</strong> No
     * hay ningun {@code companybillingprofile.*} sembrado —sembrarlo es un
     * changeset, y esta feature no trae ninguno—, y de los codigos que existen el
     * que describe esto es «administrar los datos de la empresa». Queda anotado
     * como deuda: el permiso propio permitiria dar de alta la ficha sin abrir
     * ademas el resto de la configuracion de la empresa.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.update') and"
            + " @authz.isMyCompany(#command.companyId))")
    CompanyBillingProfileDto execute(OpenCompanyBillingProfileCommand command);
}
