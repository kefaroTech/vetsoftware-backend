package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.in.CancelSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelSupplierInvoiceService implements CancelSupplierInvoiceUseCase {
    private final SupplierInvoiceRepository repository;

    public CancelSupplierInvoiceService(SupplierInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SupplierInvoiceDto execute(Long id, Long companyId, Long actorId) {
        SupplierInvoice invoice = repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new SupplierInvoiceNotFoundException(id));
        invoice.cancel(actorId, invoice.getVersion());
        return SupplierInvoiceDto.from(repository.save(invoice));
    }
}
