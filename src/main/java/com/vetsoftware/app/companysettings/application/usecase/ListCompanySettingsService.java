package com.vetsoftware.app.companysettings.application.usecase;

import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import com.vetsoftware.app.companysettings.application.port.in.ListCompanySettingsUseCase;
import com.vetsoftware.app.companysettings.application.port.out.CompanySettingRepository;
import java.util.List;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "companySettings.list")
@Service
public class ListCompanySettingsService implements ListCompanySettingsUseCase {

    private final CompanySettingRepository repository;

    public ListCompanySettingsService(CompanySettingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CompanySettingDto> listByCompany(Long companyId) {
        return repository.findByCompany(companyId).stream().map(CompanySettingDto::from).toList();
    }
}
