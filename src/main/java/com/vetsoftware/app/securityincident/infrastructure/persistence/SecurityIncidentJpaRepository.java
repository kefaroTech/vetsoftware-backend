package com.vetsoftware.app.securityincident.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query} de {@code UPDATE} ni de {@code DELETE}, y
 * no es que aun no hayan hecho falta.</strong> Las dos escrituras que editan
 * una fila —el reporte y el cierre— van por el ciclo leer-modificar-guardar de
 * una entidad gestionada, que es el unico camino que {@code @Version} protege.
 * Un {@code UPDATE} masivo aqui pasaria de largo del bloqueo optimista y
 * dejaria la fila cambiada con su version intacta: el {@code save} concurrente
 * que llegara con la version vieja casaria igual y pisaria el reporte, sin
 * excepcion y sin log ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}).
 *
 * <p>
 * Ningun metodo recibe {@code companyId} porque la tabla no tiene esa columna.
 * El listado hereda {@code findAll(Pageable)} de {@code JpaRepository} y su
 * gate esta en el puerto de entrada, cerrado a {@code ROLE_SYSTEM} a secas.
 */
public interface SecurityIncidentJpaRepository
        extends
            JpaRepository<SecurityIncidentJpaEntity, Long> {
}
