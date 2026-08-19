package com.vetsoftware.app.companysettings.infrastructure.persistence;

import com.vetsoftware.app.companysettings.application.port.out.CompanySettingRepository;
import com.vetsoftware.app.companysettings.domain.CompanySetting;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCompanySettingRepository implements CompanySettingRepository {

    private final CompanySettingJpaRepository jpaRepository;

    public JpaCompanySettingRepository(CompanySettingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CompanySetting save(CompanySetting setting) {
        return toDomain(jpaRepository.save(toJpa(setting)));
    }

    @Override
    public Optional<CompanySetting> find(Long companyId, String propertyName) {
        return jpaRepository.findByCompanyIdAndPropertyName(companyId, propertyName)
                .map(JpaCompanySettingRepository::toDomain);
    }

    @Override
    public List<CompanySetting> findByCompany(Long companyId) {
        return jpaRepository.findByCompanyId(companyId).stream()
                .map(JpaCompanySettingRepository::toDomain).toList();
    }

    private static CompanySettingJpaEntity toJpa(CompanySetting s) {
        CompanySettingJpaEntity e = new CompanySettingJpaEntity();
        e.setId(s.getId());
        e.setCompanyId(s.getCompanyId());
        e.setPropertyName(s.getPropertyName());
        e.setValue(s.getValue());
        e.setCreatedDate(s.getCreatedDate());
        e.setVersion(s.getVersion());
        e.setEnabled(s.isEnabled());
        return e;
    }

    private static CompanySetting toDomain(CompanySettingJpaEntity e) {
        return new CompanySetting(e.getId(), e.getCompanyId(), e.getPropertyName(), e.getValue(),
                e.getCreatedDate(), e.getVersion(), e.isEnabled());
    }
}
