package com.vetsoftware.app.dianprovider.application.usecase;

import com.vetsoftware.app.dianprovider.application.command.UpdateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.in.UpdateDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.application.port.out.DianProviderConfigRepository;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfigNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "dian.provider.config.update")
@Service
public class UpdateDianProviderConfigService implements UpdateDianProviderConfigUseCase {
    private final DianProviderConfigRepository repository;

    public UpdateDianProviderConfigService(DianProviderConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DianProviderConfigDto execute(UpdateDianProviderConfigCommand command) {
        DianProviderConfig config = repository.findByCompanyId(command.companyId())
                .orElseThrow(() -> new DianProviderConfigNotFoundException(command.companyId()));
        config.update(command.provider(), command.baseUrl(), command.clientId(),
                command.clientSecret(), command.username(), command.password(), command.apiToken(),
                command.webhookSecret(), command.numberingProviderRef());
        return DianProviderConfigDto.from(repository.save(config));
    }
}
