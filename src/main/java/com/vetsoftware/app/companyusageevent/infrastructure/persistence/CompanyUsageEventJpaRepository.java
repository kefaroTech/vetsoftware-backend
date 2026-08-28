package com.vetsoftware.app.companyusageevent.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}, y menos aun de {@code UPDATE} o
 * {@code DELETE}.</strong> Las cinco consultas son derivadas, que es lo que
 * corresponde cuando no hay nada que expresar que el nombre del metodo no diga.
 *
 * <p>
 * La ausencia de escrituras masivas no es que aun no hayan hecho falta. La
 * unica escritura que edita una fila es colgarle el cargo, y va por el ciclo
 * leer-modificar-guardar de una entidad gestionada, que es el <em>unico</em>
 * camino que {@code @Version} protege. Un {@code UPDATE} masivo aqui pasaria de
 * largo del bloqueo optimista y dejaria la fila cambiada con su version
 * intacta: el {@code save} concurrente que llegara con la version vieja casaria
 * igual y pisaria el cargo, sin excepcion y sin log
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}, incidencia #53). Si algun dia
 * hiciera falta una, tendria que <b>nombrar la empresa en su {@code WHERE}</b>
 * ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}) y mover {@code version} en su
 * {@code SET}.
 *
 * <p>
 * <strong>Ningun metodo devuelve {@code List}.</strong> Sobre una tabla cuya
 * proyeccion son doce millones de filas, un listado sin paginar es un
 * {@code SELECT *} que reventaria de noche, dentro del proceso de cierre.
 *
 * <p>
 * <strong>Ni un {@code @EntityGraph}</strong>: la entidad no tiene una sola
 * asociacion —las siete claves foraneas van como escalares, ver
 * {@code CompanyUsageEventJpaEntity}—, asi que no hay N+1 que evitar.
 */
public interface CompanyUsageEventJpaRepository
        extends
            JpaRepository<CompanyUsageEventJpaEntity, Long> {

    /**
     * La carga acotada por empresa: la que impide que el cierre de una clinica
     * toque el hecho de otra ({@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     */
    Optional<CompanyUsageEventJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    Page<CompanyUsageEventJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    Page<CompanyUsageEventJpaEntity> findAllByCompanyIdAndChargeId(Long companyId, Long chargeId,
            Pageable pageable);
}
