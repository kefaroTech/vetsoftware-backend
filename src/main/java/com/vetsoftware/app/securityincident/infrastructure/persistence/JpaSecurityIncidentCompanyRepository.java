package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentCompanyRepository;
import com.vetsoftware.app.securityincident.domain.AffectedScope;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * <strong>No implementa borrado porque el puerto no lo declara</strong>, y el
 * puerto no lo declara porque quitar una clinica de la lista de afectados
 * destruye la prueba de que se le notifico.
 */
@Repository
public class JpaSecurityIncidentCompanyRepository implements SecurityIncidentCompanyRepository {

    private final SecurityIncidentCompanyJpaRepository jpaRepository;
    private final SecurityIncidentJpaRepository incidentJpaRepository;
    private final SecurityIncidentCompanyJpaMapper mapper;

    public JpaSecurityIncidentCompanyRepository(SecurityIncidentCompanyJpaRepository jpaRepository,
            SecurityIncidentJpaRepository incidentJpaRepository,
            SecurityIncidentCompanyJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.incidentJpaRepository = incidentJpaRepository;
        this.mapper = mapper;
    }

    /**
     * {@code getReferenceById} devuelve un proxy sin {@code SELECT}: el caso de uso
     * ya comprobo que el incidente existe, asi que volver a leerlo seria pagar dos
     * veces por el mismo dato.
     *
     * <p>
     * La vuelta al dominio <b>reusa el id que ya teniamos</b> en lugar de leerlo
     * del proxy, que lo inicializaria y dispararia justo la consulta que el proxy
     * evita.
     */
    @Override
    public SecurityIncidentCompany save(SecurityIncidentCompany affected) {
        SecurityIncidentJpaEntity incident = incidentJpaRepository
                .getReferenceById(affected.getSecurityIncidentId());
        SecurityIncidentCompanyJpaEntity saved = jpaRepository
                .save(mapper.toJpa(affected, incident));
        return mapper.toDomain(saved, affected.getSecurityIncidentId());
    }

    @Override
    public PageResult<SecurityIncidentCompany> findByIncidentId(Long securityIncidentId, int page,
            int pageSize) {
        return Pages.result(jpaRepository.findByIncident_Id(securityIncidentId,
                Pages.request(page, pageSize, affectedOrder())), mapper::toDomain);
    }

    @Override
    public boolean existsByIncidentIdAndCompanyIdAndScope(Long securityIncidentId, Long companyId,
            AffectedScope affectedScope) {
        return jpaRepository.existsByIncident_IdAndCompanyIdAndAffectedScope(securityIncidentId,
                companyId, affectedScope);
    }

    /**
     * Por clinica y despues por ambito, con el {@code id} de desempate: la misma
     * clinica puede aparecer dos veces con dos ambitos, asi que ordenar solo por
     * empresa no seria un orden total.
     */
    private static Sort affectedOrder() {
        return Sort.by(Sort.Order.asc("companyId"), Sort.Order.asc("affectedScope"),
                Sort.Order.asc("id"));
    }
}
