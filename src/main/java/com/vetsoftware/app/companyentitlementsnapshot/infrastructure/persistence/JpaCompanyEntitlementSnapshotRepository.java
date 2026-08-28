package com.vetsoftware.app.companyentitlementsnapshot.infrastructure.persistence;

import com.vetsoftware.app.companyentitlementsnapshot.application.port.out.CompanyEntitlementSnapshotRepository;
import com.vetsoftware.app.companyentitlementsnapshot.domain.CompanyEntitlementSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de las fotos de permisos. Solo agrega. */
@Repository
public class JpaCompanyEntitlementSnapshotRepository
        implements
            CompanyEntitlementSnapshotRepository {

    private final CompanyEntitlementSnapshotJpaRepository jpaRepository;
    private final CompanyEntitlementSnapshotJpaMapper mapper;

    public JpaCompanyEntitlementSnapshotRepository(
            CompanyEntitlementSnapshotJpaRepository jpaRepository,
            CompanyEntitlementSnapshotJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CompanyEntitlementSnapshot append(CompanyEntitlementSnapshot snapshot) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(snapshot)));
    }

    @Override
    public Optional<CompanyEntitlementSnapshot> findLatestAsOf(Long companyId, LocalDateTime at) {
        return jpaRepository
                .findFirstByCompanyIdAndRecalculatedAtLessThanEqualOrderByRecalculatedAtDescIdDesc(
                        companyId, at)
                .map(mapper::toDomain);
    }

    @Override
    public List<CompanyEntitlementSnapshot> findAllByCompanyIdBetween(Long companyId,
            LocalDateTime from, LocalDateTime to) {
        return jpaRepository
                .findAllByCompanyIdAndRecalculatedAtBetweenOrderByRecalculatedAtAscIdAsc(companyId,
                        from, to)
                .stream().map(mapper::toDomain).toList();
    }
}
