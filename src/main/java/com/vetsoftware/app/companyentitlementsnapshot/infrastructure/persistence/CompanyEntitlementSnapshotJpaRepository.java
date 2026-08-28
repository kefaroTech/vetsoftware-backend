package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consultas de las fotos, acotadas por empresa.
 *
 * <p>
 * La consulta «qué veía el día X» recorre
 * {@code ix_company_entitlement_snapshots_company}: igualdad por empresa y
 * rango descendente por fecha, un salto y una fila.
 *
 * <p>
 * Sin consultas de escritura: la tabla solo se agrega.
 */
public interface CompanyEntitlementSnapshotJpaRepository
        extends
            JpaRepository<CompanyEntitlementSnapshotJpaEntity, Long> {

    Optional<CompanyEntitlementSnapshotJpaEntity> findFirstByCompanyIdAndRecalculatedAtLessThanEqualOrderByRecalculatedAtDescIdDesc(
            Long companyId, LocalDateTime at);

    List<CompanyEntitlementSnapshotJpaEntity> findAllByCompanyIdAndRecalculatedAtBetweenOrderByRecalculatedAtAscIdAsc(
            Long companyId, LocalDateTime from, LocalDateTime to);
}
