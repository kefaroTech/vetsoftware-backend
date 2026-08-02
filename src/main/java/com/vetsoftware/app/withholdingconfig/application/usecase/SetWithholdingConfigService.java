package com.vetsoftware.app.withholdingconfig.application.usecase;

import com.vetsoftware.app.withholdingconfig.application.command.SetWithholdingConfigCommand;
import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import com.vetsoftware.app.withholdingconfig.application.port.in.SetWithholdingConfigUseCase;
import com.vetsoftware.app.withholdingconfig.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.withholdingconfig.application.port.out.WithholdingConfigRepository;
import com.vetsoftware.app.withholdingconfig.domain.CompanyRef;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "withholding.config.set")
@Service
public class SetWithholdingConfigService implements SetWithholdingConfigUseCase {
    private final WithholdingConfigRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public SetWithholdingConfigService(WithholdingConfigRepository repository,
            CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public WithholdingConfigDto execute(SetWithholdingConfigCommand command) {
        WithholdingConfig config = repository.findByCompanyId(command.companyId()).map(existing -> {
            existing.update(command.reteFuenteRate(), command.reteIvaRate(), command.reteIcaRate());
            return existing;
        }).orElseGet(() -> {
            CompanyRef company = companyQueryPort.findById(command.companyId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Company not found: " + command.companyId()));
            return WithholdingConfig.create(company, command.reteFuenteRate(),
                    command.reteIvaRate(), command.reteIcaRate());
        });
        return WithholdingConfigDto.from(repository.save(config));
    }
}
