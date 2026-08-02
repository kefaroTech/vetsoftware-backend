package com.vetsoftware.app.withholdingconfig.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.withholdingconfig.application.port.out.WithholdingConfigRepository;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWithholdingConfigRepository implements WithholdingConfigRepository {
    private final WithholdingConfigJpaRepository jpaRepository;
    private final WithholdingConfigJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaWithholdingConfigRepository(WithholdingConfigJpaRepository jpaRepository,
            WithholdingConfigJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public WithholdingConfig save(WithholdingConfig config) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(config.getCompany().id());
        WithholdingConfigJpaEntity saved = jpaRepository.save(mapper.toJpa(config, company));
        return mapper.toDomain(saved, config.getCompany());
    }

    @Override
    public Optional<WithholdingConfig> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompany_Id(companyId).map(mapper::toDomain);
    }
}
