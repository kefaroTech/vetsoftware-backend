package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanyTaxProfileRepository implements CompanyTaxProfileRepository {
    private final CompanyTaxProfileJpaRepository jpaRepository;
    private final CompanyTaxProfileJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;
    private final EconomicActivityJpaRepository economicActivityJpaRepository;

    public JpaCompanyTaxProfileRepository(CompanyTaxProfileJpaRepository jpaRepository,
            CompanyTaxProfileJpaMapper mapper, CompanyJpaRepository companyJpaRepository,
            EconomicActivityJpaRepository economicActivityJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
        this.economicActivityJpaRepository = economicActivityJpaRepository;
    }

    @Override
    public CompanyTaxProfile save(CompanyTaxProfile profile) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(profile.getCompany().id());
        EconomicActivityJpaEntity economicActivity = profile.getEconomicActivity() == null
                ? null
                : economicActivityJpaRepository
                        .getReferenceById(profile.getEconomicActivity().id());
        CompanyTaxProfileJpaEntity saved = jpaRepository
                .save(mapper.toJpa(profile, company, economicActivity));
        return mapper.toDomain(saved, profile.getCompany(), profile.getEconomicActivity());
    }

    @Override
    public Optional<CompanyTaxProfile> findByCompanyId(Long companyId) {
        return jpaRepository.findByCompany_Id(companyId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCompanyId(Long companyId) {
        return jpaRepository.existsByCompany_Id(companyId);
    }

    @Override
    public void delete(Long companyId) {
        jpaRepository.deleteByCompanyId(companyId);
    }

    @Override
    public int reactivate(Long companyId) {
        return jpaRepository.reactivate(companyId);
    }
}
