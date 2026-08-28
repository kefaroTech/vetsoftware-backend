package com.vetsoftware.app.securityincident.application.port.out;

import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * <strong>Ningun metodo recibe {@code companyId}, y esa ausencia es una
 * afirmacion sobre el modelo.</strong> {@code security_incidents} no tiene
 * columna de empresa: el incidente es de la plataforma y el reparto por clinica
 * vive en la puente. Anadir aqui un {@code findAllByCompanyId} exigiria
 * inventarse la columna.
 *
 * <p>
 * <strong>Sin borrado.</strong> Un incidente se cierra, no se retira: la fila
 * es la prueba de que se reporto, y de cuando.
 */
public interface SecurityIncidentRepository {

    SecurityIncident save(SecurityIncident incident);

    Optional<SecurityIncident> findById(Long id);

    /** El barrido de plataforma, paginado. Lo mas reciente primero. */
    PageResult<SecurityIncident> findAll(int page, int pageSize);
}
