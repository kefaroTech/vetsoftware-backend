package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.command.CreateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.in.CreateSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.BranchQueryPort;
import com.vetsoftware.app.supplierinvoice.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.CompanyRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNumberAlreadyExistsException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSupplierInvoiceService implements CreateSupplierInvoiceUseCase {
    private final SupplierInvoiceRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final SupplierQueryPort supplierQueryPort;

    public CreateSupplierInvoiceService(SupplierInvoiceRepository repository,
                                        CompanyQueryPort companyQueryPort,
                                        BranchQueryPort branchQueryPort,
                                        SupplierQueryPort supplierQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.branchQueryPort = branchQueryPort;
        this.supplierQueryPort = supplierQueryPort;
    }

    @Override
    @Transactional
    public SupplierInvoiceDto execute(CreateSupplierInvoiceCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        BranchRef branch = branchQueryPort.findByIdAndCompanyId(command.branchId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + command.branchId()));
        SupplierRef supplier = supplierQueryPort.findByIdAndCompanyId(command.supplierId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + command.supplierId()));

        if (repository.existsByCompanySupplierAndNumber(
                command.companyId(), command.supplierId(), command.invoiceNumber())) {
            throw new SupplierInvoiceNumberAlreadyExistsException(command.invoiceNumber());
        }

        SupplierInvoice invoice = SupplierInvoice.create(company, branch, supplier,
            command.purchaseOrderId(), command.goodsReceiptId(), command.invoiceNumber(),
            command.issueDate(), command.dueDate(), command.subtotal(), command.taxAmount(),
            nz(command.withholdingAmount()), command.notes(), command.actorId());
        return SupplierInvoiceDto.from(repository.save(invoice));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
