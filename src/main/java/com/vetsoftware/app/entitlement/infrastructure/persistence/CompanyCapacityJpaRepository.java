package com.vetsoftware.app.entitlement.infrastructure.persistence;

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
    List<CompanyCapacityJpaEntity> findAllByCompany_IdOrderByCapacityUnitAsc(Long companyId);

    @EntityGraph(attributePaths = "company")
    Optional<CompanyCapacityJpaEntity> findByCompany_IdAndCapacityUnit(Long companyId,
            String capacityUnit);

    /**
     * Mueve el consumo en el motor, en una sola sentencia.
     *
     * <p>
     * Nunca leer-modificar-guardar desde Java: dos altas simultaneas leerian el
     * mismo valor y una de las dos se perderia sin excepcion y sin log.
     *
     * <p>
     * El {@code WHERE} nombra la empresa
     * --{@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}-- y ademas se niega a dejar el
     * consumo en negativo: con {@code chk_company_capacities_quantities} detras, un
     * delta pasado del revés seria un error de motor a mitad de transaccion en vez
     * de cero filas afectadas, que es lo que el caso de uso sabe interpretar. No
     * mueve {@code version} porque esta tabla no la lleva
     * ({@code E6_YA_PROTEGIDO}).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE company_capacities
            SET used_quantity = used_quantity + :delta
            WHERE company_id = :companyId
              AND capacity_unit = :capacityUnit
              AND used_quantity + :delta >= 0
              AND (:delta <= 0 OR used_quantity + :delta <= limit_quantity)
            """, nativeQuery = true)
    int addUsage(@Param("companyId") Long companyId, @Param("capacityUnit") String capacityUnit,
            @Param("delta") int delta);
}
