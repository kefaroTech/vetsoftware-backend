package com.vetsoftware.app.supplier.application.usecase;

import com.vetsoftware.app.supplier.application.command.UpdateSupplierCommand;
import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.UpdateSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import com.vetsoftware.app.supplier.domain.SupplierNameAlreadyExistsException;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "supplier.update")
@Service
public class UpdateSupplierService implements UpdateSupplierUseCase {
  private final SupplierRepository repository;
  private final CompanyQueryPort companyQueryPort;

  public UpdateSupplierService(SupplierRepository repository, CompanyQueryPort companyQueryPort) {
    this.repository = repository;
    this.companyQueryPort = companyQueryPort;
  }

  @Override
  @Transactional
  public SupplierDto execute(UpdateSupplierCommand command) {
    Supplier supplier =
        repository
            .findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new SupplierNotFoundException(command.id()));
    CompanyRef company =
        companyQueryPort
            .findById(command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
    if (repository.existsByCompanyIdAndNameExcludingId(
        command.companyId(), command.name(), command.id())) {
      throw new SupplierNameAlreadyExistsException(command.name());
    }

    supplier.update(
        command.name(),
        command.taxId(),
        command.contactName(),
        command.phone(),
        command.email(),
        command.address(),
        command.paymentTermsDays(),
        command.notes(),
        company,
        command.updatedBy(),
        command.version());
    return SupplierDto.from(repository.save(supplier));
  }
}
