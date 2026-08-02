package com.vetsoftware.app.dianprovider.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.dianprovider.application.port.out.DianProviderConfigRepository;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDianProviderConfigRepository implements DianProviderConfigRepository {
    private final DianProviderConfigJpaRepository jpaRepository;
    private final DianProviderConfigJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaDianProviderConfigRepository(DianProviderConfigJpaRepository jpaRepository,
            DianProviderConfigJpaMapper mapper, CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public DianProviderConfig save(DianProviderConfig config) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(config.getCompany().id());
        DianProviderConfigJpaEntity saved = jpaRepository.save(mapper.toJpa(config, company));
        return mapper.toDomain(saved, config.getCompany());
    }

    @Override
    public Optional<DianProviderConfig> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompany_Id(companyId).map(mapper::toDomain);
    }
}
