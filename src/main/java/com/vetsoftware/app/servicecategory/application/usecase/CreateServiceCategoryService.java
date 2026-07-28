package com.vetsoftware.app.servicecategory.application.usecase;

import com.vetsoftware.app.servicecategory.application.command.CreateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.in.CreateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNameAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "service.category.create")
@Service
public class CreateServiceCategoryService implements CreateServiceCategoryUseCase {
    private final ServiceCategoryRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public CreateServiceCategoryService(ServiceCategoryRepository repository,
                                        CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    public ServiceCategoryDto execute(CreateServiceCategoryCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndName(command.companyId(), command.name())) {
            throw new ServiceCategoryNameAlreadyExistsException(command.name());
        }
        return ServiceCategoryDto.from(
                repository.save(ServiceCategory.create(command.name(), command.description(), company)));
    }
}
