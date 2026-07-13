package com.vetsoftware.app.goodsreceipt.application.usecase;

import com.vetsoftware.app.goodsreceipt.application.port.in.DeleteGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.out.GoodsReceiptRepository;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceipt;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "goods_receipt.delete")
@Service
public class DeleteGoodsReceiptService implements DeleteGoodsReceiptUseCase {
    private final GoodsReceiptRepository repository;

    public DeleteGoodsReceiptService(GoodsReceiptRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        GoodsReceipt receipt = repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new GoodsReceiptNotFoundException(id));
        if (receipt.getStatus() != GoodsReceiptStatus.DRAFT) {
            throw new InvalidGoodsReceiptStatusTransitionException(
                "Only DRAFT goods receipts can be deleted, current status: " + receipt.getStatus());
        }
        repository.delete(id);
    }
}
