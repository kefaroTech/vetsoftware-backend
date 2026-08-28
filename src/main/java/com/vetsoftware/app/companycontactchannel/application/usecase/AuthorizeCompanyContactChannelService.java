package com.vetsoftware.app.companycontactchannel.application.usecase;

import com.vetsoftware.app.companycontactchannel.application.command.AuthorizeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.in.AuthorizeCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deja constancia de que la empresa autorizo un canal para un fin concreto.
 *
 * <p>
 * <strong>El {@code Clock} va inyectado por constructor y no es una
 * formalidad.</strong> {@code authorized_at} es la fecha desde la que el
 * consentimiento vale: es la que decide, meses despues, si un aviso ya enviado
 * estaba permitido. Un {@code LocalDateTime.now()} pelado la haria intestable
 * —el caso se caeria solo el dia que el reloj cruce medianoche entre dos
 * lineas— y, peor, dejaria sin sitio la unica alternativa que hay que negar por
 * escrito: que la fecha llegue del cliente.
 *
 * <p>
 * <strong>No hay comprobacion de canal duplicado, y no falta.</strong> El
 * esquema no declara unicidad sobre {@code (company_id, address, purpose)} a
 * proposito: autorizar hoy el mismo correo que se revoco en marzo es una
 * autorizacion <em>nueva</em>, con su fecha y su evidencia, y la anterior tiene
 * que seguir existiendo tal como quedo. Rechazar el duplicado obligaria a
 * reabrir la fila vieja, que es exactamente lo que una bitacora probatoria no
 * puede hacer.
 */
@Observed(name = "company.contact.channel.authorize")
@Service
public class AuthorizeCompanyContactChannelService
        implements
            AuthorizeCompanyContactChannelUseCase {

    private final CompanyContactChannelRepository repository;
    private final Clock clock;

    public AuthorizeCompanyContactChannelService(CompanyContactChannelRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyContactChannelDto execute(AuthorizeCompanyContactChannelCommand command) {
        CompanyContactChannel channel = CompanyContactChannel.authorize(command.companyId(),
                command.channelType(), command.address(), command.purpose(),
                command.authorizationEvidence(), LocalDateTime.now(clock));
        return CompanyContactChannelDto.from(repository.save(channel));
    }
}
