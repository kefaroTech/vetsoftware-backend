package com.vetsoftware.app.supplier.application.usecase;

import com.vetsoftware.app.supplier.application.dto.SupplierDto;
import com.vetsoftware.app.supplier.application.port.in.ReactivateSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "supplier.reactivate")
@Service
public class ReactivateSupplierService implements ReactivateSupplierUseCase {
  private final SupplierRepository repository;

  public ReactivateSupplierService(SupplierRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SupplierDto execute(Long id, Long companyId) {
    int rows = repository.reactivate(id, companyId);
    if (rows == 0) throw new SupplierNotFoundException(id);
    return SupplierDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new SupplierNotFoundException(id)));
  }
}
