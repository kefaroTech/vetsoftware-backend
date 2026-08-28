package com.vetsoftware.app.companytrialgrant.infrastructure.persistence;

import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de las concesiones de prueba. */
@Repository
public class JpaCompanyTrialGrantRepository implements CompanyTrialGrantRepository {

    private final CompanyTrialGrantJpaRepository jpaRepository;
    private final CompanyTrialGrantJpaMapper mapper;

    public JpaCompanyTrialGrantRepository(CompanyTrialGrantJpaRepository jpaRepository,
            CompanyTrialGrantJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CompanyTrialGrant save(CompanyTrialGrant grant) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(grant)));
    }

    @Override
    public Optional<CompanyTrialGrant> findByCompanyIdAndCatalogItemId(Long companyId,
            Long catalogItemId) {
        return jpaRepository.findByCompanyIdAndCatalogItemId(companyId, catalogItemId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByCompanyIdAndCatalogItemId(Long companyId, Long catalogItemId) {
        return jpaRepository.existsByCompanyIdAndCatalogItemId(companyId, catalogItemId);
    }

    @Override
    public List<CompanyTrialGrant> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyIdOrderByGrantedOnAscIdAsc(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<CompanyTrialGrant> findLiveExpiredOn(LocalDate day) {
        return jpaRepository.findLiveExpiredOn(day).stream().map(mapper::toDomain).toList();
    }
}
