package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import org.springframework.stereotype.Component;

/**
 * Traduce la puente de afectados.
 *
 * <p>
 * <strong>No hay version que copiar</strong>: la tabla no la tiene
 * ({@code E2_TABLA_PUENTE}), porque la fila se escribe una vez y no se
 * reescribe.
 *
 * <p>
 * {@code toJpa} recibe el incidente ya resuelto —un proxy de
 * {@code getReferenceById}, sin {@code SELECT}— en vez de buscarlo: es el
 * patron de escritura del CLAUDE.md y evita una consulta por fila insertada.
 */
@Component
public class SecurityIncidentCompanyJpaMapper {

    public SecurityIncidentCompanyJpaEntity toJpa(SecurityIncidentCompany affected,
            SecurityIncidentJpaEntity incident) {
        SecurityIncidentCompanyJpaEntity entity = new SecurityIncidentCompanyJpaEntity();
        entity.setId(affected.getId());
        entity.setIncident(incident);
        entity.setCompanyId(affected.getCompanyId());
        entity.setAffectedScope(affected.getAffectedScope());
        entity.setAffectedSubjectCount(affected.getAffectedSubjectCount());
        return entity;
    }

    /**
     * Camino de lectura: el {@code @EntityGraph} del repositorio ya hidrato el
     * incidente, asi que leer su id no dispara ninguna consulta.
     */
    public SecurityIncidentCompany toDomain(SecurityIncidentCompanyJpaEntity entity) {
        return new SecurityIncidentCompany(entity.getId(), entity.getIncident().getId(),
                entity.getCompanyId(), entity.getAffectedScope(), entity.getAffectedSubjectCount());
    }

    /**
     * Camino de escritura: reusa el id que el llamador ya tenia en la mano en vez
     * de tocar {@code entity.getIncident()}, que sobre el proxy de
     * {@code getReferenceById} inicializaria el objeto y dispararia el
     * {@code SELECT} que ese proxy existe para evitar.
     */
    public SecurityIncidentCompany toDomain(SecurityIncidentCompanyJpaEntity entity,
            Long securityIncidentId) {
        return new SecurityIncidentCompany(entity.getId(), securityIncidentId,
                entity.getCompanyId(), entity.getAffectedScope(), entity.getAffectedSubjectCount());
    }
}
