package com.vetsoftware.app.companycontactchannel.application.port.out;

import com.vetsoftware.app.companycontactchannel.domain.CompanyContactChannel;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de esta
 * rodaja lleva la empresa, y ninguna de las consultas de conjunto se sirve sin
 * ella —{@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}—.
 *
 * <p>
 * <strong>Y no hay borrado, ni logico ni fisico.</strong> Ni {@code delete}, ni
 * {@code disable}, ni reactivacion. Un canal que deja de valer se revoca y
 * queda: esta tabla es una bitacora probatoria, y una prueba que se puede
 * desactivar no prueba nada.
 */
public interface CompanyContactChannelRepository {

    /**
     * <strong>Escribe y vacia la sesion en el acto.</strong> Ver
     * {@code JpaCompanyContactChannelRepository.save}: el indice unico del canal
     * primario se comprueba sentencia a sentencia, asi que el relevo del primario
     * exige que la bajada del incumbente llegue al motor antes que la subida del
     * sucesor. Con el volcado diferido, Hibernate ordena los dos {@code UPDATE}
     * como quiera y el relevo falla de forma intermitente.
     */
    CompanyContactChannel save(CompanyContactChannel channel);

    Optional<CompanyContactChannel> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * El canal principal vivo de ese proposito, si lo hay.
     *
     * <p>
     * Es exactamente la fila que ocupa el hueco de
     * {@code uq_company_contact_channels_primary}: {@code is_primary = TRUE} y
     * {@code revoked_at IS NULL}, que son las dos condiciones de la columna
     * generada. Un canal revocado que conserva el marcador <strong>no</strong> sale
     * por aqui, porque ya no ocupa el hueco.
     */
    Optional<CompanyContactChannel> findPrimaryByCompanyIdAndPurpose(Long companyId,
            ContactPurpose purpose);

    /**
     * La consulta caliente: por donde se le puede escribir hoy a esta empresa para
     * este fin.
     *
     * <p>
     * Recorre {@code ix_company_contact_channels_usable (company_id, purpose,
     * revoked_at)} en el mismo orden en que el indice esta escrito.
     */
    PageResult<CompanyContactChannel> findAllUsableByCompanyIdAndPurpose(Long companyId,
            ContactPurpose purpose, int page, int pageSize);

    /**
     * La bitacora completa de la empresa, revocados incluidos. Es la vista que hace
     * falta para responder si un aviso de hace ocho meses estaba permitido.
     */
    PageResult<CompanyContactChannel> findAllByCompanyId(Long companyId, int page, int pageSize);
}
