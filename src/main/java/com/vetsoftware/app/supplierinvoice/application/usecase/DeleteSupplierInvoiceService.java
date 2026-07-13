package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.port.in.DeleteSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteSupplierInvoiceService implements DeleteSupplierInvoiceUseCase {
    private final SupplierInvoiceRepository repository;

    public DeleteSupplierInvoiceService(SupplierInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new SupplierInvoiceNotFoundException(id));
        repository.delete(id);
    }
}
