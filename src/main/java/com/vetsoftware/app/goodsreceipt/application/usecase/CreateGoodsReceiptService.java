package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.command.CreateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.application.command.GoodsReceiptLineCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.CreateGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.BranchQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.ProductQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.goodsreceipt.domain.BranchRef;
import com.vetsoftware.app.goodsreceipt.domain.CompanyRef;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import com.vetsoftware.app.goodsreceipt.domain.SupplierRef;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "goods_receipt.create")
@Service
public class CreateGoodsReceiptService implements CreateGoodsReceiptUseCase {
    private final GoodsReceiptRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final BranchQueryPort branchQueryPort;
    private final SupplierQueryPort supplierQueryPort;
    private final ProductQueryPort productQueryPort;

    public CreateGoodsReceiptService(GoodsReceiptRepository repository,
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
    public GoodsReceiptDto execute(CreateGoodsReceiptCommand command) {
        Long companyId = command.companyId();
        CompanyRef company = companyQueryPort.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
        BranchRef branch = branchQueryPort.findById(command.branchId(), companyId)
            .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + command.branchId()));
        SupplierRef supplier = supplierQueryPort.findById(command.supplierId(), companyId)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + command.supplierId()));

        // Si purchaseOrderId != null NO se lee la orden aquí: la validación real ocurre al confirmar.
        List<GoodsReceiptLine> lines = buildLines(command.lines(), companyId);

        GoodsReceipt receipt = GoodsReceipt.create(
            company, branch, supplier, command.purchaseOrderId(), command.receiptDate(),
            command.supplierInvoiceNumber(), command.notes(), lines, command.createdBy());
        return GoodsReceiptDto.from(repository.save(receipt));
    }

    private List<GoodsReceiptLine> buildLines(List<GoodsReceiptLineCommand> lineCommands, Long companyId) {
        if (lineCommands == null || lineCommands.isEmpty()) {
            throw new IllegalArgumentException("at least one line is required");
        }
        return lineCommands.stream().map(lc -> {
            ProductRef product = productQueryPort.findById(lc.productId(), companyId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + lc.productId()));
            return GoodsReceiptLine.create(product, lc.purchaseOrderLineId(), lc.lotNumber(),
                lc.expireDate(), lc.quantityReceived(), lc.unitCost());
        }).toList();
    }
}
