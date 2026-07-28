package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.command.CreatePurchaseOrderCommand;
import com.vetsoftware.app.purchaseorder.application.command.PurchaseOrderLineCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.in.CreatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.BranchQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.ProductQueryPort;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.purchaseorder.domain.BranchRef;
import com.vetsoftware.app.purchaseorder.domain.CompanyRef;
import com.vetsoftware.app.purchaseorder.domain.ProductRef;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderLine;
import com.vetsoftware.app.purchaseorder.domain.SupplierRef;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "purchase.order.create")
@Service
public class CreatePurchaseOrderService implements CreatePurchaseOrderUseCase {
    private final PurchaseOrderRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final SupplierQueryPort supplierQueryPort;
    private final ProductQueryPort productQueryPort;

    public CreatePurchaseOrderService(PurchaseOrderRepository repository,
                                      CompanyQueryPort companyQueryPort,
                                      BranchQueryPort branchQueryPort,
                                      SupplierQueryPort supplierQueryPort,
                                      ProductQueryPort productQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.branchQueryPort = branchQueryPort;
        this.supplierQueryPort = supplierQueryPort;
        this.productQueryPort = productQueryPort;
    }

    @Override
    @Transactional
    public PurchaseOrderDto execute(CreatePurchaseOrderCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        BranchRef branch = branchQueryPort.findById(command.branchId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + command.branchId()));
        SupplierRef supplier = supplierQueryPort.findById(command.supplierId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + command.supplierId()));

        List<PurchaseOrderLine> lines = toLines(command.lines(), command.companyId());

        PurchaseOrder order = PurchaseOrder.create(company, branch, supplier,
            command.orderDate(), command.expectedDate(), command.notes(), lines, command.createdBy());
        return PurchaseOrderDto.from(repository.save(order));
    }

    private List<PurchaseOrderLine> toLines(List<PurchaseOrderLineCommand> lineCommands, Long companyId) {
        if (lineCommands == null) return List.of();
        return lineCommands.stream().map(l -> {
            ProductRef product = productQueryPort.findById(l.productId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + l.productId()));
            return PurchaseOrderLine.create(product, l.quantityOrdered(), l.unitCost());
        }).toList();
    }
}
