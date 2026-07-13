package com.vetsoftware.app.companysettings.application.port.out;

import com.vetsoftware.app.companysettings.domain.CompanySetting;
import java.util.List;
import java.util.Optional;

public interface CompanySettingRepository {
    CompanySetting save(CompanySetting setting);
    Optional<CompanySetting> find(Long companyId, String propertyName);
    List<CompanySetting> findByCompany(Long companyId);
}
