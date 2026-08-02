package com.vetsoftware.app.supplierinvoice.application.usecase;

import com.vetsoftware.app.supplierinvoice.application.command.UpdateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.dto.SupplierInvoiceDto;
import com.vetsoftware.app.supplierinvoice.application.port.in.UpdateSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.out.BranchQueryPort;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierInvoiceRepository;
import com.vetsoftware.app.supplierinvoice.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.supplierinvoice.domain.BranchRef;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoice;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNumberAlreadyExistsException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierRef;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "supplier.invoice.update")
@Service
public class UpdateSupplierInvoiceService implements UpdateSupplierInvoiceUseCase {
    private final SupplierInvoiceRepository repository;
    private final BranchQueryPort branchQueryPort;
    private final SupplierQueryPort supplierQueryPort;

    public UpdateSupplierInvoiceService(SupplierInvoiceRepository repository,
            BranchQueryPort branchQueryPort, SupplierQueryPort supplierQueryPort) {
        this.repository = repository;
        this.branchQueryPort = branchQueryPort;
        this.supplierQueryPort = supplierQueryPort;
    }

    @Override
    @Transactional
    public SupplierInvoiceDto execute(UpdateSupplierInvoiceCommand command) {
        SupplierInvoice invoice = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SupplierInvoiceNotFoundException(command.id()));
        BranchRef branch = branchQueryPort
                .findByIdAndCompanyId(command.branchId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Branch not found: " + command.branchId()));
        SupplierRef supplier = supplierQueryPort
                .findByIdAndCompanyId(command.supplierId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Supplier not found: " + command.supplierId()));

        boolean numberOrSupplierChanged = !invoice.getInvoiceNumber()
                .equals(command.invoiceNumber())
                || !invoice.getSupplier().id().equals(command.supplierId());
        if (numberOrSupplierChanged && repository.existsByCompanySupplierAndNumber(
                command.companyId(), command.supplierId(), command.invoiceNumber())) {
            throw new SupplierInvoiceNumberAlreadyExistsException(command.invoiceNumber());
        }

        invoice.update(branch, supplier, command.purchaseOrderId(), command.goodsReceiptId(),
                command.invoiceNumber(), command.issueDate(), command.dueDate(), command.subtotal(),
                command.taxAmount(), nz(command.withholdingAmount()), command.notes(),
                command.actorId(), command.version());
        return SupplierInvoiceDto.from(repository.save(invoice));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
