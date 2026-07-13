package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.in.FindSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FindSupplierInvoiceService implements FindSupplierInvoiceUseCase {
    private final SupplierInvoiceRepository repository;

    public FindSupplierInvoiceService(SupplierInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierInvoiceDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
            .map(SupplierInvoiceDto::from)
            .orElseThrow(() -> new SupplierInvoiceNotFoundException(id));
    }
}
