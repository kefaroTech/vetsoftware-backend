package com.vetsoftware.app.companylimitoverride.infrastructure.persistence;

import com.vetsoftware.app.companylimitoverride.application.port.out.CompanyLimitOverrideRepository;
import com.vetsoftware.app.companylimitoverride.domain.CompanyLimitOverride;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de las excepciones negociadas. */
@Repository
public class JpaCompanyLimitOverrideRepository implements CompanyLimitOverrideRepository {

    private final CompanyLimitOverrideJpaRepository jpaRepository;
    private final CompanyLimitOverrideJpaMapper mapper;

    public JpaCompanyLimitOverrideRepository(CompanyLimitOverrideJpaRepository jpaRepository,
            CompanyLimitOverrideJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public CompanyLimitOverride save(CompanyLimitOverride override) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(override)));
    }

    @Override
    public Optional<CompanyLimitOverride> findAliveByCompanyIdAndLimitDimensionId(Long companyId,
            Long limitDimensionId) {
        return jpaRepository.findByCompanyIdAndLimitDimensionIdAndRevokedAtIsNullAndValidToIsNull(
                companyId, limitDimensionId).map(mapper::toDomain);
    }

    @Override
    public Optional<CompanyLimitOverride> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
    }

    @Override
    public boolean existsAliveByCompanyIdAndLimitDimensionId(Long companyId,
            Long limitDimensionId) {
        return jpaRepository.existsByCompanyIdAndLimitDimensionIdAndRevokedAtIsNullAndValidToIsNull(
                companyId, limitDimensionId);
    }

    @Override
    public List<CompanyLimitOverride> findAllByCompanyId(Long companyId) {
        return jpaRepository
                .findAllByCompanyIdOrderByLimitDimensionIdAscValidFromDescIdDesc(companyId).stream()
                .map(mapper::toDomain).toList();
    }
}
