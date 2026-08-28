package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.securityincident.application.port.out.SecurityIncidentRepository;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSecurityIncidentRepository implements SecurityIncidentRepository {

    private final SecurityIncidentJpaRepository jpaRepository;
    private final SecurityIncidentJpaMapper mapper;

    public JpaSecurityIncidentRepository(SecurityIncidentJpaRepository jpaRepository,
            SecurityIncidentJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * <strong>{@code saveAndFlush} y no {@code save}.</strong> La respuesta lleva
     * la fila tal como quedo, y {@code save} no incrementa {@code @Version} hasta
     * el flush: sin forzarlo aqui, el objeto devuelto llevaria la version anterior
     * y quien la reenviara para la siguiente edicion chocaria contra un bloqueo
     * optimista que no habia por que perder.
     *
     * <p>
     * Ademas hace que los ocho {@code CHECK} del changeset 356 fallen
     * <em>dentro</em> de esta llamada y no en un commit posterior, donde la
     * excepcion ya no sabe decir que operacion la provoco.
     */
    @Override
    public SecurityIncident save(SecurityIncident incident) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpa(incident)));
    }

    @Override
    public Optional<SecurityIncident> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<SecurityIncident> findAll(int page, int pageSize) {
        return Pages.result(jpaRepository.findAll(Pages.request(page, pageSize, incidentOrder())),
                mapper::toDomain);
    }

    /**
     * Lo mas reciente primero, con el {@code id} de desempate.
     *
     * <p>
     * El desempate no es adorno: dos incidentes pueden compartir
     * {@code detected_at} —un barrido que detecta varias cosas a la vez los sella
     * con el mismo instante— y sin un criterio estable dos paginas consecutivas
     * repiten u omiten filas.
     */
    private static Sort incidentOrder() {
        return Sort.by(Sort.Order.desc("detectedAt"), Sort.Order.desc("id"));
    }
}
