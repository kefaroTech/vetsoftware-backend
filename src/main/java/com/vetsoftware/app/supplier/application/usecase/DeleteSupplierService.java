package com.vetsoftware.app.supplier.application.usecase;

import com.vetsoftware.app.supplier.application.port.in.DeleteSupplierUseCase;
import com.vetsoftware.app.supplier.application.port.out.SupplierRepository;
import com.vetsoftware.app.supplier.domain.SupplierNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "supplier.delete")
@Service
public class DeleteSupplierService implements DeleteSupplierUseCase {
    private final SupplierRepository repository;

    public DeleteSupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new SupplierNotFoundException(id));
        repository.delete(id);
    }
}
