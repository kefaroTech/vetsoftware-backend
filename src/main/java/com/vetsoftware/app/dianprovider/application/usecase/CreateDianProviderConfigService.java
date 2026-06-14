package com.vetsoftware.app.dianprovider.application.usecase;

import com.vetsoftware.app.dianprovider.application.command.CreateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.in.CreateDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.dianprovider.application.port.out.DianProviderConfigRepository;
import com.vetsoftware.app.dianprovider.domain.CompanyRef;
import com.vetsoftware.app.dianprovider.domain.DianProviderConfig;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "dianProviderConfig.create")
@Service
public class CreateDianProviderConfigService implements CreateDianProviderConfigUseCase {
    private final DianProviderConfigRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateDianProviderConfigService(DianProviderConfigRepository repository,
                                           CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public DianProviderConfigDto execute(CreateDianProviderConfigCommand command) {
        if (repository.findByCompanyId(command.companyId()).isPresent()) {
            throw new IllegalStateException("La empresa ya tiene una configuracion de proveedor DIAN.");
        }
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        DianProviderConfig config = DianProviderConfig.create(company, command.provider(), command.environment(),
                command.baseUrl(), command.clientId(), command.clientSecret(), command.username(),
                command.password(), command.apiToken(), command.webhookSecret(), command.numberingProviderRef());
        return DianProviderConfigDto.from(repository.save(config));
    }
}
