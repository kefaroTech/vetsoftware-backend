package com.vetsoftware.app.companyactivitymonth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <strong>Sin una sola {@code @Query}, ni de lectura ni de escritura.</strong>
 * Las seis consultas de esta rodaja son derivadas, y eso no es casualidad: las
 * derivadas no pueden proyectar un literal booleano, que es el defecto que
 * {@code PROYECCION_SIN_LITERAL_BOOLEANO} persigue —un
 * {@code CASE WHEN COUNT(c) > 0 THEN TRUE} lo tipa Hibernate 7 como
 * {@code Integer} y falla el 100 % de las veces—.
 *
 * <p>
 * <strong>Y sin ningun {@code UPDATE} ni {@code DELETE} masivo, tampoco por
 * casualidad.</strong> La unica escritura que toca una fila existente es el
 * recalculo del mes, y va por el ciclo leer-modificar-guardar de la entidad
 * gestionada, que es el unico camino que {@code @Version} protege. Un
 * {@code UPDATE} de conjunto aqui pasaria de largo del bloqueo optimista y
 * dejaria la fila cambiada con su version intacta
 * ({@code UPDATE_MASIVO_MUEVE_LA_VERSION}). Como la tabla ademas lleva
 * {@code company_id}, ese mismo {@code UPDATE} tendria que nombrar la empresa
 * en su {@code WHERE} ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}): dos reglas
 * duras que no hay que sortear si simplemente no existe la consulta.
 */
public interface CompanyActivityMonthJpaRepository
        extends
            JpaRepository<CompanyActivityMonthJpaEntity, Long> {

    /**
     * Como mucho una fila: lo garantiza {@code uq_cam_month (company_id,
     * period_key)}.
     */
    Optional<CompanyActivityMonthJpaEntity> findByCompanyIdAndPeriodKey(Long companyId,
            String periodKey);

    Page<CompanyActivityMonthJpaEntity> findAllByCompanyId(Long companyId, Pageable pageable);

    Page<CompanyActivityMonthJpaEntity> findAllByPeriodKey(String periodKey, Pageable pageable);

    /**
     * El barrido de dormidos. {@code ix_cam_dormant (period_key, active_days)} lo
     * sirve entero: una igualdad y un rango, en ese orden.
     */
    Page<CompanyActivityMonthJpaEntity> findAllByPeriodKeyAndActiveDaysLessThanEqual(
            String periodKey, int activeDaysThreshold, Pageable pageable);
}
