package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.command.GoodsReceiptLineCommand;
import com.vetsoftware.app.goodsreceipt.application.command.UpdateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.UpdateGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.BranchQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.application.port.out.ProductQueryPort;
import com.vetsoftware.app.goodsreceipt.application.port.out.SupplierQueryPort;
import com.vetsoftware.app.goodsreceipt.domain.BranchRef;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptLine;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import com.vetsoftware.app.goodsreceipt.domain.SupplierRef;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "goods.receipt.update")
@Service
public class UpdateGoodsReceiptService implements UpdateGoodsReceiptUseCase {
    private final GoodsReceiptRepository repository;
    private final BranchQueryPort branchQueryPort;
    private final SupplierQueryPort supplierQueryPort;
    private final ProductQueryPort productQueryPort;

    public UpdateGoodsReceiptService(GoodsReceiptRepository repository,
                                     BranchQueryPort branchQueryPort,
                                     SupplierQueryPort supplierQueryPort,
                                     ProductQueryPort productQueryPort) {
        this.repository = repository;
        this.branchQueryPort = branchQueryPort;
        this.supplierQueryPort = supplierQueryPort;
        this.productQueryPort = productQueryPort;
    }

    @Override
    @Transactional
    public GoodsReceiptDto execute(UpdateGoodsReceiptCommand command) {
        Long companyId = command.companyId();
        GoodsReceipt receipt = repository.findByIdAndCompanyId(command.id(), companyId)
            .orElseThrow(() -> new GoodsReceiptNotFoundException(command.id()));
        BranchRef branch = branchQueryPort.findById(command.branchId(), companyId)
            .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + command.branchId()));
        SupplierRef supplier = supplierQueryPort.findById(command.supplierId(), companyId)
            .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + command.supplierId()));
        List<GoodsReceiptLine> lines = buildLines(command.lines(), companyId);

        // La guarda de "solo editable en DRAFT" vive en el agregado (lanza InvalidGoodsReceiptStatusTransitionException).
        receipt.update(branch, supplier, command.purchaseOrderId(), command.receiptDate(),
            command.supplierInvoiceNumber(), command.notes(), lines, command.updatedBy(), command.version());
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
