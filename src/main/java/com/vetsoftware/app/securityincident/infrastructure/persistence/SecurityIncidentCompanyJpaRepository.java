package com.vetsoftware.app.securityincident.infrastructure.persistence;

import com.vetsoftware.app.securityincident.domain.AffectedScope;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Los {@code @EntityGraph} no son adorno.</strong> La asociacion con el
 * incidente es {@code LAZY} y el mapper de lectura le pide el id: sin el grafo,
 * una pagina de veinte afectados dispara veintiuna consultas.
 *
 * <p>
 * <strong>Sin borrado.</strong> {@code JpaRepository} hereda {@code delete},
 * pero ni el puerto de salida ni el adaptador lo exponen: quitar una clinica de
 * la lista de afectados destruye la prueba de que se le notifico.
 *
 * <p>
 * Sin {@code @Query} de escritura: la fila se inserta y no se toca mas.
 */
public interface SecurityIncidentCompanyJpaRepository
        extends
            JpaRepository<SecurityIncidentCompanyJpaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "incident")
    List<SecurityIncidentCompanyJpaEntity> findAll();

    @Override
    @EntityGraph(attributePaths = "incident")
    Optional<SecurityIncidentCompanyJpaEntity> findById(Long id);

    @EntityGraph(attributePaths = "incident")
    Page<SecurityIncidentCompanyJpaEntity> findByIncident_Id(Long incidentId, Pageable pageable);

    /** Espejo de {@code uq_sic_pair}: la terna, no el par. */
    boolean existsByIncident_IdAndCompanyIdAndAffectedScope(Long incidentId, Long companyId,
            AffectedScope affectedScope);
}
