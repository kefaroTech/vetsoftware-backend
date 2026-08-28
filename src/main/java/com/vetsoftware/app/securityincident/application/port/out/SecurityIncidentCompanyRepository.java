package com.vetsoftware.app.securityincident.application.port.out;

import com.vetsoftware.app.securityincident.domain.AffectedScope;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.shared.pagination.PageResult;

/**
 * La puente de afectados.
 *
 * <p>
 * <strong>No declara borrado y no lo va a declarar.</strong> Quitar una clinica
 * de la lista destruye la prueba de que se le notifico, que es justamente para
 * lo que existe la tabla. La ausencia del metodo es la decision, escrita donde
 * se ve.
 *
 * <p>
 * Tampoco declara {@code save} de actualizacion: la fila se escribe una vez, al
 * cerrar el incidente, y por eso la tabla va exenta de {@code @Version} con el
 * codigo {@code E2_TABLA_PUENTE}.
 */
public interface SecurityIncidentCompanyRepository {

    SecurityIncidentCompany save(SecurityIncidentCompany affected);

    /**
     * Las clinicas alcanzadas por un incidente, paginadas.
     *
     * <p>
     * Devuelve filas de varias empresas sin filtrar por ninguna, y eso solo lo
     * puede servir {@code ROLE_SYSTEM} a secas: el gate esta en
     * {@code ListAffectedCompaniesUseCase} y no aqui, que es donde
     * {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM} lo va a buscar.
     */
    PageResult<SecurityIncidentCompany> findByIncidentId(Long securityIncidentId, int page,
            int pageSize);

    /**
     * Espejo de {@code uq_sic_pair}. Sirve para dar el conflicto con su nombre
     * —{@code AffectedCompanyAlreadyRegisteredException}— en vez de dejar salir una
     * violacion de integridad cruda.
     */
    boolean existsByIncidentIdAndCompanyIdAndScope(Long securityIncidentId, Long companyId,
            AffectedScope affectedScope);
}
