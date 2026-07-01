package com.vetsoftware.app.servicecategory.application.usecase;

import com.vetsoftware.app.servicecategory.application.command.UpdateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.in.UpdateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceCategoryRepository;
import com.vetsoftware.app.servicecategory.domain.CompanyRef;
import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNameAlreadyExistsException;
import com.vetsoftware.app.servicecategory.domain.ServiceCategoryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_category.update")
@Service
public class UpdateServiceCategoryService implements UpdateServiceCategoryUseCase {
    private final ServiceCategoryRepository repository;
    private final CompanyQueryPort companyQueryPort;

    public UpdateServiceCategoryService(ServiceCategoryRepository repository,
                                        CompanyQueryPort companyQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
    }

    @Override
    @Transactional
    public ServiceCategoryDto execute(UpdateServiceCategoryCommand command) {
        ServiceCategory serviceCategory = repository.findById(command.id())
                .orElseThrow(() -> new ServiceCategoryNotFoundException(command.id()));
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyIdAndNameExcludingId(command.companyId(), command.name(), command.id())) {
            throw new ServiceCategoryNameAlreadyExistsException(command.name());
        }
        serviceCategory.update(command.name(), command.description(), company, command.updatedBy());
        return ServiceCategoryDto.from(repository.save(serviceCategory));
    }
}
