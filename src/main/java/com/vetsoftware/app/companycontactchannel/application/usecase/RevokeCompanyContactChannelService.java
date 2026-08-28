package com.vetsoftware.app.companycontactchannel.application.usecase;

import com.vetsoftware.app.companycontactchannel.application.command.RevokeCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.in.RevokeCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra un canal: escribe {@code revoked_at} y {@code revoked_reason} y deja
 * la fila donde estaba.
 *
 * <p>
 * <strong>No hay un {@code DeleteCompanyContactChannelService}, y esa ausencia
 * es la feature.</strong> Un canal borrado demuestra lo contrario de lo que
 * hace falta demostrar: que el aviso de marzo iba a una direccion autorizada en
 * marzo. Aqui no hay borrado fisico ni logico —la tabla ni siquiera tiene
 * {@code enabled}—, solo un cierre fechado y motivado.
 *
 * <p>
 * <strong>Revocar libera ademas el hueco de primario</strong>, y lo hace sin
 * que este servicio toque {@code is_primary}: la columna generada
 * {@code primary_marker} vale {@code NULL} en cuanto hay {@code revoked_at},
 * asi que el canal deja de ocupar {@code uq_company_contact_channels_primary}
 * solo. Bajar aqui el marcador seria borrar la constancia de que ese era el
 * canal principal mientras estuvo vivo.
 */
@Observed(name = "company.contact.channel.revoke")
@Service
public class RevokeCompanyContactChannelService implements RevokeCompanyContactChannelUseCase {

    private final CompanyContactChannelRepository repository;
    private final Clock clock;

    public RevokeCompanyContactChannelService(CompanyContactChannelRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * La carga va acotada por empresa porque el puerto de salida no ofrece otra
     * cosa: el {@code id} lo escribe el cliente en la URL y la anotacion del puerto
     * solo prueba que declara su propia empresa, no de quien es la fila
     * ({@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     */
    @Override
    @Transactional
    public CompanyContactChannelDto execute(RevokeCompanyContactChannelCommand command) {
        CompanyContactChannel channel = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new CompanyContactChannelNotFoundException(command.id()));
        channel.revoke(LocalDateTime.now(clock), command.reason());
        return CompanyContactChannelDto.from(repository.save(channel));
    }
}
