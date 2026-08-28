package com.vetsoftware.app.companycontactchannel.infrastructure.persistence;

import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}.</strong> Las cuatro consultas de la
 * feature las expresa el derivador de nombres de Spring Data, asi que aqui no
 * hay SQL que pueda olvidarse de mover la {@code version} en su {@code SET}
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, #53) ni un {@code UPDATE} o
 * {@code DELETE} al que {@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA} tenga que
 * pedirle el filtro de empresa. Toda escritura pasa por el ciclo
 * leer-modificar-guardar, que es el unico camino que {@code @Version} protege.
 *
 * <p>
 * <strong>Y esa decision aqui vale doble.</strong> Lo natural al escribir el
 * relevo del canal primario es un {@code UPDATE ... SET is_primary = FALSE
 * WHERE company_id = :companyId AND purpose = :purpose}, que ademas es una
 * escritura masiva sobre una tabla versionada: iria directa a la base sin
 * comprobar ni incrementar nada, y el {@code save} concurrente que llegara con
 * la version vieja pisaria el cambio en silencio. El relevo se hace en cambio
 * con dos entidades gestionadas, en el orden que el indice unico exige.
 *
 * <p>
 * <strong>Sin {@code @EntityGraph}</strong>, y no por descuido: la entidad no
 * tiene ni una asociacion, asi que no hay N+1 que evitar.
 */
public interface CompanyContactChannelJpaRepository
        extends
            JpaRepository<CompanyContactChannelJpaEntity, Long> {

    Optional<CompanyContactChannelJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * El canal que ocupa hoy el hueco de
     * {@code uq_company_contact_channels_primary} para ese proposito.
     *
     * <p>
     * Las dos condiciones del final —{@code is_primary = TRUE} y
     * {@code revoked_at IS NULL}— son <strong>exactamente</strong> las de la
     * columna generada {@code primary_marker}. Si alguien quitara aqui la de la
     * revocacion, el relevo bajaria el marcador de un canal ya cerrado —que no
     * estorba a nadie— y dejaria intacto al que si ocupa el hueco: el
     * {@code UPDATE} del sucesor moriria con un duplicado que no explica nada.
     */
    Optional<CompanyContactChannelJpaEntity> findByCompanyIdAndPurposeAndPrimaryIsTrueAndRevokedAtIsNull(
            Long companyId, ContactPurpose purpose);

    /**
     * La consulta caliente. El {@code WHERE} nombra las tres columnas de
     * {@code ix_company_contact_channels_usable (company_id, purpose, revoked_at)},
     * en ese orden.
     */
    Page<CompanyContactChannelJpaEntity> findAllByCompanyIdAndPurposeAndRevokedAtIsNull(
            Long companyId, ContactPurpose purpose, Pageable pageable);

    /** La bitacora completa de la empresa, revocados incluidos. */
    Page<CompanyContactChannelJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);
}
