package com.vetsoftware.app.externalinvoicingoutage.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query} de {@code UPDATE} ni de {@code DELETE}, y
 * no es que aun no hayan hecho falta.</strong> Las dos escrituras que editan
 * una fila —el cierre de la caida y la marca de aviso— van por el ciclo
 * leer-modificar-guardar de una entidad gestionada, que es el unico camino que
 * {@code @Version} protege. Un {@code UPDATE} masivo aqui pasaria de largo del
 * bloqueo optimista y dejaria la fila cambiada con su version intacta: el
 * {@code save} concurrente que llegara con la version vieja casaria igual y
 * pisaria el cierre, sin excepcion y sin log
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * <strong>Las dos consultas son derivadas y no llevan JPQL, tambien a
 * proposito.</strong> {@code PROYECCION_SIN_LITERAL_BOOLEANO} prohibe proyectar
 * un {@code CASE WHEN ... THEN TRUE} —Hibernate 7 lo tipa como {@code Integer}
 * y falla el 100 % de las veces—, y la forma de no rozar siquiera esa trampa es
 * no escribir la consulta.
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna:
 * es un registro global de plataforma.
 */
public interface ExternalInvoicingOutageJpaRepository
        extends
            JpaRepository<ExternalInvoicingOutageJpaEntity, Long> {

    /**
     * Las caidas todavia vivas.
     *
     * <p>
     * Se apoya en {@code ix_eio_open (ended_at, started_at)}, y funciona con
     * {@code IS NULL}: MySQL indexa los nulos y los busca por indice —lo que no
     * existe en este motor son indices parciales—.
     */
    List<ExternalInvoicingOutageJpaEntity> findByEndedAtIsNull(Sort sort);

    @Override
    Page<ExternalInvoicingOutageJpaEntity> findAll(Pageable pageable);
}
