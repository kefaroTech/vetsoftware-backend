package com.vetsoftware.app.withholdingconfig.application.usecase;

import com.vetsoftware.app.withholdingconfig.application.dto.WithholdingConfigDto;
import com.vetsoftware.app.withholdingconfig.application.port.in.FindWithholdingConfigUseCase;
import com.vetsoftware.app.withholdingconfig.application.port.out.WithholdingConfigRepository;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfigNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "withholdingConfig.find")
@Service
public class FindWithholdingConfigService implements FindWithholdingConfigUseCase {
    private final WithholdingConfigRepository repository;

    public FindWithholdingConfigService(WithholdingConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    public WithholdingConfigDto findByCompany(Long companyId) {
        return WithholdingConfigDto.from(repository.findByCompanyId(companyId)
                .orElseThrow(() -> new WithholdingConfigNotFoundException(companyId)));
    }
}
