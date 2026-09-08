package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.command.CreateServiceCommand;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.CreateServiceUseCase;
import com.vetsoftware.app.service.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceCategoryQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.application.port.out.ServiceUsageLimitPort;
import com.vetsoftware.app.service.application.port.out.TaxQueryPort;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.TaxRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service.create")
@org.springframework.stereotype.Service
public class CreateServiceService implements CreateServiceUseCase {
    private final ServiceRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final ServiceCategoryQueryPort serviceCategoryQueryPort;
    private final TaxQueryPort taxQueryPort;
    private final ServiceUsageLimitPort usageLimitPort;

    public CreateServiceService(ServiceRepository repository, CompanyQueryPort companyQueryPort,
            ServiceCategoryQueryPort serviceCategoryQueryPort, TaxQueryPort taxQueryPort,
            ServiceUsageLimitPort usageLimitPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.serviceCategoryQueryPort = serviceCategoryQueryPort;
        this.taxQueryPort = taxQueryPort;
        this.usageLimitPort = usageLimitPort;
    }

    @Override
    @Transactional
    public ServiceDto execute(CreateServiceCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        ServiceCategoryRef serviceCategory = serviceCategoryQueryPort
                .findById(command.serviceCategoryId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ServiceCategory not found: " + command.serviceCategoryId()));
        TaxRef tax = command.taxId() == null
                ? null
                : taxQueryPort.findById(command.taxId(), command.companyId()).orElseThrow(
                        () -> new IllegalArgumentException("Tax not found: " + command.taxId()));
        usageLimitPort.checkNotExceeded(command.companyId());
        Service service = Service.create(command.name(), command.price(), command.taxTreatment(),
                command.notes(), serviceCategory, tax, company);
        return ServiceDto.from(repository.save(service));
    }
}
