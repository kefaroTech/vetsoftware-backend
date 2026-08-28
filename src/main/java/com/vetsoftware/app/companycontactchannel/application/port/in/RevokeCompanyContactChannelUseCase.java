package com.vetsoftware.app.companycontactchannel.application.port.in;

import com.vetsoftware.app.companycontactchannel.application.command.RevokeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RevokeCompanyContactChannelUseCase {

    /**
     * Cierra el canal dejando escrito cuando y por que.
     *
     * <p>
     * <strong>No es un borrado disfrazado</strong>: la fila se queda y sigue
     * saliendo en la bitacora. Lo que cambia es que a partir de {@code revoked_at}
     * ya no se le puede escribir por ahi.
     *
     * <p>
     * <strong>El {@code id} lo escribe el cliente en la URL, asi que el
     * {@code companyId} viaja siempre</strong> y la carga va acotada por el
     * (BE-COV). La anotacion sola no bastaria: prueba que el atacante declara su
     * propia empresa, no de quien es la fila que senala el {@code id}.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyContactChannel.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    CompanyContactChannelDto execute(RevokeCompanyContactChannelCommand command);
}
