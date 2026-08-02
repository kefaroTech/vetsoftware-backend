package com.vetsoftware.app.supplier.application.usecase;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.FindSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "supplier.find")
@Service
public class FindSupplierService implements FindSupplierUseCase {
  private final SupplierRepository repository;

  public FindSupplierService(SupplierRepository repository) {
    this.repository = repository;
  }

  @Override
  public SupplierDto findById(Long id, Long companyId) {
    return SupplierDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new SupplierNotFoundException(id)));
  }
}
