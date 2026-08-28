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

    /**
     * <strong>{@code saveAndFlush} y no {@code save}: es el contrato del puerto, no
     * una preferencia.</strong> La sucesion cierra el perfil vigente y abre el
     * siguiente en la misma transaccion, e Hibernate ejecuta <em>todos</em> los
     * {@code INSERT} antes que los {@code UPDATE}: sin el flush intermedio los dos
     * calcularian el mismo {@code current_profile_marker} y
     * {@code uq_company_tax_profiles_current} pararia la operacion con un
     * {@code Duplicate entry} sobre una columna que nadie escribio.
     */
    @Override
    public CompanyTaxProfile save(CompanyTaxProfile profile) {
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(profile.getCompany().id());
        EconomicActivityJpaEntity economicActivity = profile.getEconomicActivity() == null
                ? null
                : economicActivityJpaRepository
                        .getReferenceById(profile.getEconomicActivity().id());
        CompanyTaxProfileJpaEntity saved = jpaRepository
                .saveAndFlush(mapper.toJpa(profile, company, economicActivity));
        return mapper.toDomain(saved, profile.getCompany(), profile.getEconomicActivity());
    }

    @Override
    public int close(CompanyTaxProfile profile) {
        return jpaRepository.closeCurrent(profile.getId(), profile.getCompany().id(),
                profile.getValidTo());
    }

    @Override
    public Optional<CompanyTaxProfile> findCurrentByCompanyId(Long companyId) {
        return jpaRepository.findCurrentByCompanyId(companyId).map(mapper::toDomain);
    }

    @Override
    public boolean existsCurrentByCompanyId(Long companyId) {
        return jpaRepository.existsCurrentByCompanyId(companyId);
    }
}
