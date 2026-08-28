package com.vetsoftware.app.entitlement.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de {@code company_capacities}, todas acotadas por empresa. */
public interface CompanyCapacityJpaRepository
        extends
            JpaRepository<CompanyCapacityJpaEntity, Long> {

    @EntityGraph(attributePaths = "company")
    List<CompanyCapacityJpaEntity> findAllByCompany_IdOrderByLimitDimensionIdAscPeriodKeyAsc(
            Long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<CompanyCapacityJpaEntity> findByCompany_IdAndLimitDimensionIdAndPeriodKey(
            Long companyId, Long limitDimensionId, String periodKey);

    /**
     * Mueve el consumo en el motor, en una sola sentencia (R-LIMIT-01).
     *
     * <p>
     * Nunca leer-modificar-guardar desde Java: dos altas simultaneas leerian el
     * mismo valor y una de las dos se perderia sin excepcion y sin log.
     *
     * <p>
     * El {@code WHERE} nombra la empresa
     * --{@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}--, nombra el eje y el periodo
     * --que juntos son la clave del contador-- y ademas se niega a dejar el consumo
     * en negativo: con {@code chk_company_capacities_quantities} detras, un delta
     * pasado del reves seria un error de motor a mitad de transaccion en vez de
     * cero filas afectadas, que es lo que el caso de uso sabe interpretar. No mueve
     * {@code version} porque esta tabla no la lleva ({@code E6_YA_PROTEGIDO}).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE company_capacities
            SET used_quantity = used_quantity + :delta
            WHERE company_id = :companyId
              AND limit_dimension_id = :limitDimensionId
              AND period_key = :periodKey
              AND used_quantity + :delta >= 0
              AND (:delta <= 0 OR used_quantity + :delta <= limit_quantity)
            """, nativeQuery = true)
    int addUsage(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId, @Param("periodKey") String periodKey,
            @Param("delta") int delta);

    /**
     * La misma instruccion atomica de {@link #addUsage} <strong>menos la clausula
     * del techo</strong>: el camino del excedente.
     *
     * <p>
     * Solo se llega aqui con un permiso escrito en {@code subscription_item_limits}
     * ({@code enforcement = 'OVERAGE'} y precio por unidad positivo). El
     * {@code WHERE} sigue nombrando la empresa
     * ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}), el eje y el periodo, y sigue
     * negandose a dejar el consumo en negativo. No mueve {@code version} porque
     * esta tabla no la lleva ({@code E6_YA_PROTEGIDO}).
     *
     * <p>
     * <strong>Es una consulta gemela y no un parametro</strong>: saltarse el techo
     * tiene que tener nombre propio y salir en el diff, no esconderse tras un
     * booleano que cualquier llamador nuevo puede pasar del reves.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE company_capacities
            SET used_quantity = used_quantity + :delta
            WHERE company_id = :companyId
              AND limit_dimension_id = :limitDimensionId
              AND period_key = :periodKey
              AND used_quantity + :delta >= 0
            """, nativeQuery = true)
    int addUsageAllowingOverage(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId, @Param("periodKey") String periodKey,
            @Param("delta") int delta);

    /**
     * El permiso de excedente vigente de una empresa sobre un eje, si lo hay.
     *
     * <p>
     * <strong>Nativa y contra {@code subscription_item_limits}</strong>, que es de
     * otra rodaja: el cruce vive aqui, en {@code infrastructure/persistence}, que
     * es el unico sitio donde el vertical slicing lo permite. Devuelve una
     * proyeccion y no una entidad para no traerse el agregado de la otra feature.
     *
     * <p>
     * <strong>{@code enabled = TRUE} va explicito en las dos tablas</strong>: una
     * consulta nativa no pasa por el {@code @SQLRestriction} de las entidades, y
     * sin el se leeria el permiso de una linea de contrato ya dada de baja.
     *
     * <p>
     * <strong>{@code LIMIT 1} con orden total.</strong>
     * {@code uq_subscription_item_limits (company_id, subscription_item_id,
     * limit_dimension_id)} no impide que una empresa tenga <em>dos</em> lineas
     * vivas sobre el mismo eje —con tramos acumulativos es el caso normal—, asi que
     * la pregunta «¿puedo pasarme?» puede tener dos respuestas. Se toma la mas
     * reciente, y el desempate por {@code id} existe para que la respuesta no
     * dependa del plan que elija el motor.
     */
    @Query(value = """
            SELECT sil.subscription_item_id AS subscriptionItemId,
                   si.subscription_id AS subscriptionId,
                   sil.overage_unit_amount AS unitAmount
            FROM subscription_item_limits sil
            JOIN subscription_items si ON si.id = sil.subscription_item_id
                                      AND si.company_id = sil.company_id
                                      AND si.enabled = TRUE
            WHERE sil.company_id = :companyId
              AND sil.limit_dimension_id = :limitDimensionId
              AND sil.enforcement = 'OVERAGE'
              AND sil.overage_unit_amount IS NOT NULL
              AND sil.enabled = TRUE
              AND si.effective_from <= :on
              AND (si.effective_to IS NULL OR si.effective_to > :on)
            ORDER BY sil.created_date DESC, sil.id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<OverageAllowanceView> findOverageAllowance(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId, @Param("on") LocalDate on);

    /**
     * Proyeccion del permiso de excedente. Los tres campos que hacen falta para
     * devengar el cargo y ninguno mas.
     *
     * <p>
     * {@code unitAmount} es {@code BigDecimal} y no {@code double}: es dinero, y un
     * binario flotante no representa exactamente un precio con dos decimales.
     */
    interface OverageAllowanceView {

        Long getSubscriptionItemId();

        Long getSubscriptionId();

        BigDecimal getUnitAmount();
    }

    /**
     * Escribe el techo derivado del contrato. <strong>La rama de actualizacion no
     * nombra {@code used_quantity}</strong>, y ese es el arreglo de #648: el
     * recalculo no tiene nada que decir sobre el consumo, asi que no lo toca. Una
     * baja de empleado que ocurra mientras el recalculo corre sobrevive.
     *
     * <p>
     * Tampoco toca {@code usage_reconciled_at}: el sello del consumo solo lo
     * escribe quien de verdad ha contado las filas (R-ENT-13). Refrescarlo aqui
     * dejaria el indicador de salud diciendo "sano" justo cuando el dato puede
     * estar mal, que es peor que no tener indicador.
     *
     * <p>
     * Es un {@code INSERT ... ON DUPLICATE KEY UPDATE} y no un {@code UPDATE} mas
     * un {@code INSERT} condicional porque asi tampoco hay carrera con el
     * nacimiento de la fila: dos recalculos simultaneos de la misma empresa no
     * pueden reventar contra {@code uq_company_capacities}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO company_capacities
                    (company_id, limit_dimension_id, measure_kind, period_key,
                     limit_quantity, used_quantity, subscription_id,
                     limit_recalculated_at, created_date)
            VALUES (:companyId, :limitDimensionId, :measureKind, :periodKey,
                    :limitQuantity, 0, :subscriptionId, :recalculatedAt, :recalculatedAt)
            ON DUPLICATE KEY UPDATE
                    limit_quantity = VALUES(limit_quantity),
                    measure_kind = VALUES(measure_kind),
                    subscription_id = VALUES(subscription_id),
                    limit_recalculated_at = VALUES(limit_recalculated_at)
            """, nativeQuery = true)
    int upsertCeiling(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId,
            @Param("measureKind") String measureKind, @Param("periodKey") String periodKey,
            @Param("limitQuantity") int limitQuantity, @Param("subscriptionId") Long subscriptionId,
            @Param("recalculatedAt") LocalDateTime recalculatedAt);

    /**
     * Hace nacer la fila del periodo de flujo que entra, heredando el techo ya
     * resuelto del periodo anterior de la misma serie (R-LIMIT-04).
     *
     * <p>
     * <strong>Una sola tabla, y ese es el punto.</strong> La alternativa --resolver
     * el techo cruzando {@code subscription_item_limits},
     * {@code catalog_item_limits} y {@code limit_dimensions} dentro de la propia
     * insercion-- pondria tres tablas en el camino mas caliente del sistema, que es
     * exactamente lo que la fila con el techo ya resuelto existe para evitar. Un
     * cupo de flujo <em>se reinicia</em>, no se renegocia: el techo del periodo
     * nuevo es el del anterior, y ese ya esta resuelto en su fila.
     *
     * <p>
     * <strong>No cuenta.</strong> Escribe {@code used_quantity = 0} y nada mas; el
     * consumo lo mueve {@link #addUsage} con su instruccion atomica, que no se
     * toca. Escribir aqui el delta convertiria el nacimiento en un segundo
     * mecanismo de conteo --justo la carrera que R-LIMIT-01 existe para cerrar--.
     *
     * <p>
     * <strong>El {@code ON DUPLICATE KEY UPDATE} es un no-op deliberado.</strong>
     * Dos peticiones simultaneas al entrar el periodo pueden intentar el nacimiento
     * a la vez; la segunda no puede reventar contra {@code uq_company_capacities}
     * ni, mucho menos, pisar el consumo que la primera ya empezo a mover. Asignar
     * la columna a si misma deja la fila intacta y devuelve cero filas afectadas.
     *
     * <p>
     * El ancestro se elige por {@code created_date} y no por {@code period_key}:
     * ordenar claves de texto mezcla granularidades --{@code 2026-12} ordena antes
     * que {@code 2026-Q1}-- y la fila mas reciente de la serie es la que de verdad
     * lleva el techo vigente. La subconsulta va como tabla derivada porque MySQL
     * exige materializar la lectura de la misma tabla en la que se inserta.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO company_capacities
                    (company_id, limit_dimension_id, measure_kind, period_key,
                     limit_quantity, used_quantity, subscription_id,
                     limit_recalculated_at, created_date)
            SELECT prev.company_id, prev.limit_dimension_id, prev.measure_kind, :periodKey,
                   prev.limit_quantity, 0, prev.subscription_id,
                   prev.limit_recalculated_at, :at
              FROM (SELECT c.company_id AS company_id,
                           c.limit_dimension_id AS limit_dimension_id,
                           c.measure_kind AS measure_kind,
                           c.limit_quantity AS limit_quantity,
                           c.subscription_id AS subscription_id,
                           c.limit_recalculated_at AS limit_recalculated_at
                      FROM company_capacities c
                     WHERE c.company_id = :companyId
                       AND c.limit_dimension_id = :limitDimensionId
                       AND c.measure_kind = 'FLOW'
                       AND c.period_key <> :periodKey
                     ORDER BY c.created_date DESC, c.id DESC
                     LIMIT 1) prev
            ON DUPLICATE KEY UPDATE
                    company_capacities.used_quantity = company_capacities.used_quantity
            """, nativeQuery = true)
    int openPeriod(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId, @Param("periodKey") String periodKey,
            @Param("at") LocalDateTime at);

    /**
     * Sella el consumo (R-ENT-13). <strong>No nombra ni {@code limit_quantity} ni
     * {@code used_quantity} ni {@code limit_recalculated_at}</strong>: quien ha
     * contado las filas reales no tiene nada que decir sobre el techo, y tocarlo
     * aqui repetiria el defecto #648 en espejo.
     *
     * <p>
     * El {@code WHERE} nombra la empresa
     * ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}). No mueve {@code version}
     * porque esta tabla no la lleva ({@code E6_YA_PROTEGIDO}).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE company_capacities
            SET usage_reconciled_at = :at
            WHERE company_id = :companyId
              AND limit_dimension_id = :limitDimensionId
              AND period_key = :periodKey
            """, nativeQuery = true)
    int markUsageReconciled(@Param("companyId") Long companyId,
            @Param("limitDimensionId") Long limitDimensionId, @Param("periodKey") String periodKey,
            @Param("at") LocalDateTime at);

    /**
     * Los contadores que el recuento periodico tiene pendientes: los que nadie ha
     * comprobado nunca y los comprobados hace demasiado.
     *
     * <p>
     * <strong>Sin filtro de empresa a proposito</strong>: es un barrido de
     * plataforma, y su unico consumidor va cerrado a {@code hasRole('SYSTEM')} a
     * secas. Se apoya en {@code ix_company_capacities_unreconciled}, que existe
     * justo para esta pregunta.
     *
     * <p>
     * <strong>Avanza por cursor de id y no por urgencia</strong>, y de eso depende
     * que el barrido termine. Un contador con desvio no se sella --a proposito,
     * para volver a mirarlo en la pasada siguiente--, asi que sigue cumpliendo este
     * {@code WHERE} despues de examinarlo: ordenando por «nunca comprobado
     * primero», los atascados ocuparian la cabeza de todos los lotes y el bucle
     * giraria sobre las mismas filas indefinidamente. Con {@code id > :afterId} una
     * pasada recorre la tabla entera pase lo que pase con cada fila.
     *
     * <p>
     * El parentesis del {@code WHERE} no es decorativo: sin el, el {@code AND} del
     * cursor se ligaria solo a la segunda rama del {@code OR} y los contadores
     * nunca comprobados se leerian una y otra vez desde el principio.
     *
     * <p>
     * Es una proyeccion y no la entidad porque necesita el codigo del eje y su
     * fecha de nacimiento, que viven en la otra tabla, y porque leer la entidad
     * obligaria a resolver {@code company} fila a fila --un N+1 dentro de un
     * barrido que recorre toda la plataforma--.
     */
    @Query(value = """
            SELECT c.id AS id, c.company_id AS companyId,
                   c.limit_dimension_id AS limitDimensionId, c.measure_kind AS measureKind,
                   c.period_key AS periodKey, c.limit_quantity AS limitQuantity,
                   c.used_quantity AS usedQuantity, c.subscription_id AS subscriptionId,
                   c.limit_recalculated_at AS limitRecalculatedAt,
                   c.usage_reconciled_at AS usageReconciledAt, c.created_date AS createdDate,
                   ld.code AS dimensionCode, ld.available_from AS availableFrom
            FROM company_capacities c
            JOIN limit_dimensions ld ON ld.id = c.limit_dimension_id AND ld.enabled = TRUE
            WHERE c.id > :afterId
              AND (c.usage_reconciled_at IS NULL OR c.usage_reconciled_at < :staleBefore)
            ORDER BY c.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<UnreconciledCapacityView> findUnreconciled(@Param("staleBefore") LocalDateTime staleBefore,
            @Param("afterId") long afterId, @Param("limit") int limit);
}
