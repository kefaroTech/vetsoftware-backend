package com.vetsoftware.app.companysettings.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySettingJpaRepository extends JpaRepository<CompanySettingJpaEntity, Long> {
    Optional<CompanySettingJpaEntity> findByCompanyIdAndPropertyName(Long companyId, String propertyName);
    List<CompanySettingJpaEntity> findByCompanyId(Long companyId);
}
