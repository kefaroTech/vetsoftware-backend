package com.vetsoftware.app.supplier.application.usecase;

import com.vetsoftware.app.supplier.application.command.CreateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.CreateSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.domain.SupplierNameAlreadyExistsException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "supplier.create")
@Service
public class CreateSupplierService implements CreateSupplierUseCase {
  private final SupplierRepository repository;
  private final CompanyQueryPort companyQueryPort;

  public CreateSupplierService(SupplierRepository repository, CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  public SupplierDto execute(CreateSupplierCommand command) {
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    if (repository.existsByCompanyIdAndName(command.companyId(), command.name())) {
      throw new SupplierNameAlreadyExistsException(command.name());
    }

    Supplier supplier =
        Supplier.create(
            command.name(),
            command.taxId(),
            command.contactName(),
            command.phone(),
            command.email(),
            command.address(),
            command.paymentTermsDays(),
            command.notes(),
            company);
    return SupplierDto.from(repository.save(supplier));
  }
}
