package com.vetsoftware.app.companysettings.application.usecase;

import com.vetsoftware.app.companysettings.application.command.SetCompanySettingCommand;
import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import com.vetsoftware.app.companysettings.application.port.in.SetCompanySettingUseCase;
import com.vetsoftware.app.companysettings.application.port.out.CompanySettingRepository;
import com.vetsoftware.app.companysettings.domain.CompanySetting;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.settings.set")
@Service
public class SetCompanySettingService implements SetCompanySettingUseCase {

    private final CompanySettingRepository repository;

    public SetCompanySettingService(CompanySettingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanySettingDto set(SetCompanySettingCommand command) {
        CompanySetting setting = repository.find(command.companyId(), command.propertyName())
                .map(existing -> {
                    existing.updateValue(command.value());
                    return existing;
                }).orElseGet(() -> CompanySetting.create(command.companyId(),
                        command.propertyName(), command.value()));
        return CompanySettingDto.from(repository.save(setting));
    }
}
