package com.vetsoftware.app.companycontactchannel.infrastructure.persistence;

import com.vetsoftware.app.companycontactchannel.application.port.out.CompanyContactChannelRepository;
import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyContactChannelRepository implements CompanyContactChannelRepository {

    private final CompanyContactChannelJpaRepository jpaRepository;
    private final CompanyContactChannelJpaMapper mapper;

    public JpaCompanyContactChannelRepository(CompanyContactChannelJpaRepository jpaRepository,
            CompanyContactChannelJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}, y esta es la linea mas
     * deliberada de la clase.</strong>
     *
     * <p>
     * El relevo del canal primario son dos escrituras que <em>tienen</em> que
     * llegar al motor en un orden concreto: primero el {@code UPDATE} que baja al
     * incumbente, despues el que sube al sucesor. Con el volcado diferido al final
     * de la transaccion, Hibernate ordena las dos actualizaciones de la misma
     * entidad como le conviene, y si la subida sale primero choca con
     * {@code uq_company_contact_channels_primary} — porque el incumbente todavia
     * ocupa el hueco en la base aunque en memoria ya no lo ocupe—. El sintoma seria
     * un duplicado intermitente que no menciona el relevo por ningun lado.
     *
     * <p>
     * El coste es un viaje mas por escritura; lo que compra es que el orden dejara
     * de depender de una heuristica de Hibernate. Y de regalo, las violaciones de
     * restriccion salen en la llamada que las provoca y no al cerrar la
     * transaccion, que es lo que permite que una prueba nombre la restriccion que
     * paro la sentencia.
     */
    @Override
    public CompanyContactChannel save(CompanyContactChannel channel) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(channel)));
    }

    @Override
    public Optional<CompanyContactChannel> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<CompanyContactChannel> findPrimaryByCompanyIdAndPurpose(Long companyId,
            ContactPurpose purpose) {
        return jpaRepository
                .findByCompanyIdAndPurposeAndPrimaryIsTrueAndRevokedAtIsNull(companyId, purpose)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<CompanyContactChannel> findAllUsableByCompanyIdAndPurpose(Long companyId,
            ContactPurpose purpose, int page, int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyIdAndPurposeAndRevokedAtIsNull(companyId,
                purpose, Pages.request(page, pageSize, elPrimarioArriba())), mapper::toDomain);
    }

    @Override
    public PageResult<CompanyContactChannel> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findAllByCompanyId(companyId,
                Pages.request(page, pageSize, loMasReciente())), mapper::toDomain);
    }

    /**
     * Canales vivos de un proposito: el primario arriba, y detras lo autorizado mas
     * recientemente.
     *
     * <p>
     * El primero de la lista es el que hay que usar, asi que ponerlo arriba es lo
     * que evita que quien consuma esta consulta tenga que recorrerla buscando el
     * marcador —y que se le olvide—. El desempate por {@code id} descendente
     * mantiene el orden total: una empresa puede autorizar varios canales del mismo
     * fin en el mismo minuto, y sin desempate dos paginas consecutivas repetirian u
     * omitirian filas.
     */
    private static Sort elPrimarioArriba() {
        return Sort.by(Sort.Direction.DESC, "primary", "authorizedAt", "id");
    }

    /**
     * Bitacora completa: lo ultimo que se autorizo primero, con el mismo desempate
     * total por {@code id}.
     */
    private static Sort loMasReciente() {
        return Sort.by(Sort.Direction.DESC, "authorizedAt", "id");
    }
}
