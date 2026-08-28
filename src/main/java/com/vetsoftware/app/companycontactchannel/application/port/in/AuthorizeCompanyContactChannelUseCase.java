package com.vetsoftware.app.companycontactchannel.application.port.in;

import com.vetsoftware.app.companycontactchannel.application.command.AuthorizeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface AuthorizeCompanyContactChannelUseCase {

    /**
     * Deja constancia de que la empresa autorizo un canal para un fin concreto.
     *
     * <p>
     * <strong>El {@code companyId} del command no lo elige el cliente</strong>: lo
     * pone el controller desde el principal, y esta anotacion lo revalida como
     * defensa en profundidad frente a cualquier otro caller. Sin ella, sembrar
     * canales de contacto en la ficha de otra empresa seria escribir por donde se
     * le avisa a la competencia.
     *
     * <p>
     * El {@code #command} del SpEL tiene que llamarse igual que el parametro: si
     * alguien renombra uno de los dos, SpEL resuelve {@code null} en silencio,
     * {@code isMyCompany(null)} devuelve {@code false} y el puerto queda cerrado
     * para todo el mundo salvo {@code hasRole('SYSTEM')} sin que nadie vea por que.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyContactChannel.create')"
            + " and @authz.isMyCompany(#command.companyId))")
    CompanyContactChannelDto execute(AuthorizeCompanyContactChannelCommand command);
}
