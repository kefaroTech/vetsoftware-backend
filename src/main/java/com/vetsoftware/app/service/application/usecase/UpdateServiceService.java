package com.vetsoftware.app.service.application.usecase;

import com.vetsoftware.app.service.application.command.UpdateServiceCommand;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.port.in.UpdateServiceUseCase;
import com.vetsoftware.app.service.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceCategoryQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.application.port.out.TaxQueryPort;
import com.vetsoftware.app.service.domain.CompanyRef;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.service.domain.ServiceNotFoundException;
import com.vetsoftware.app.service.domain.TaxRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service.update")
@org.springframework.stereotype.Service
public class UpdateServiceService implements UpdateServiceUseCase {
  private final ServiceRepository repository;
  private final CompanyQueryPort companyQueryPort;
  private final ServiceCategoryQueryPort serviceCategoryQueryPort;
  private final TaxQueryPort taxQueryPort;

  public UpdateServiceService(
      ServiceRepository repository,
      CompanyQueryPort companyQueryPort,
      ServiceCategoryQueryPort serviceCategoryQueryPort,
      TaxQueryPort taxQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
    this.serviceCategoryQueryPort = serviceCategoryQueryPort;
    this.taxQueryPort = taxQueryPort;
  }

  @Override
  @Transactional
  public ServiceDto execute(UpdateServiceCommand command) {
    Service service =
        repository
            .findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new ServiceNotFoundException(command.id()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    ServiceCategoryRef serviceCategory =
        serviceCategoryQueryPort
            .findById(command.serviceCategoryId(), command.companyId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "ServiceCategory not found: " + command.serviceCategoryId()));
    TaxRef tax =
        command.taxId() == null
            ? null
            : taxQueryPort
                .findById(command.taxId(), command.companyId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Tax not found: " + command.taxId()));
    service.update(
        command.name(),
        command.price(),
        command.taxTreatment(),
        command.notes(),
        serviceCategory,
        tax,
        company,
        command.updatedBy(),
        command.version());
    return ServiceDto.from(repository.save(service));
  }
}
