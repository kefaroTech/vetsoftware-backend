package com.vetsoftware.app.companycontactchannel.application.usecase;

import com.vetsoftware.app.companycontactchannel.application.command.DesignatePrimaryCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.application.port.in.DesignatePrimaryCompanyContactChannelUseCase;
import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannelNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relevo del canal principal de un proposito.
 *
 * <p>
 * <strong>Hay un primario por empresa Y PROPOSITO.</strong> Es la regla que mas
 * facil se copia mal: el indice unico del esquema lleva
 * {@code (primary_marker, purpose)}, no la columna generada sola, precisamente
 * para que el correo primario de facturacion y el movil primario de mora
 * convivan. Por eso el incumbente que este servicio baja se busca <em>por el
 * proposito del canal que sube</em>, y nunca por empresa a secas: hacerlo por
 * empresa dejaria a la clinica con un unico canal principal en total y
 * desarmaria la mitad del modelo.
 *
 * <p>
 * <strong>El orden de las dos escrituras es la parte fragil.</strong> El indice
 * unico se comprueba sentencia a sentencia, asi que la bajada del incumbente
 * tiene que llegar al motor <em>antes</em> que la subida del sucesor; si las
 * dos esperasen al volcado del final, Hibernate las ordenaria como quisiera y
 * el relevo fallaria de forma intermitente con un duplicado que no explica
 * nada. Lo garantiza el adaptador, cuyo {@code save} vuelca en el acto — ver
 * {@code JpaCompanyContactChannelRepository.save}.
 */
@Observed(name = "company.contact.channel.designate.primary")
@Service
public class DesignatePrimaryCompanyContactChannelService
        implements
            DesignatePrimaryCompanyContactChannelUseCase {

    private final CompanyContactChannelRepository repository;

    public DesignatePrimaryCompanyContactChannelService(
            CompanyContactChannelRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanyContactChannelDto execute(DesignatePrimaryCompanyContactChannelCommand command) {
        CompanyContactChannel channel = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new CompanyContactChannelNotFoundException(command.id()));
        if (channel.isUsable() && channel.isPrimary()) {
            return CompanyContactChannelDto.from(channel);
        }
        channel.designateAsPrimary();
        releaseIncumbent(channel);
        return CompanyContactChannelDto.from(repository.save(channel));
    }

    /**
     * Baja al primario vivo del mismo proposito, si hay otro.
     *
     * <p>
     * Se llama <em>despues</em> de marcar el sucesor en memoria y <em>antes</em> de
     * guardarlo: asi un canal revocado sale rechazado sin haber escrito nada, y el
     * {@code UPDATE} que libera el hueco sigue llegando al motor por delante del
     * que lo ocupa.
     *
     * <p>
     * El filtro por {@code id} distinto sobra en la practica —el canal que ya era
     * primario se atiende arriba— pero es lo que impide que un cambio futuro en esa
     * guarda convierta el relevo en bajarse a si mismo y volver a subirse, que es
     * un {@code UPDATE} de mas sobre una fila versionada.
     */
    private void releaseIncumbent(CompanyContactChannel promoted) {
        repository.findPrimaryByCompanyIdAndPurpose(promoted.getCompanyId(), promoted.getPurpose())
                .filter(incumbent -> !incumbent.getId().equals(promoted.getId()))
                .ifPresent(incumbent -> {
                    incumbent.releasePrimary();
                    repository.save(incumbent);
                });
    }
}
