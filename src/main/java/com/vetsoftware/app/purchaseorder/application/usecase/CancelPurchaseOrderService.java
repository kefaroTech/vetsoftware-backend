package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.in.CancelPurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "purchaseOrder.cancel")
@Service
public class CancelPurchaseOrderService implements CancelPurchaseOrderUseCase {
    private final PurchaseOrderRepository repository;

    public CancelPurchaseOrderService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PurchaseOrderDto execute(Long id, Long companyId, Long actorId) {
        PurchaseOrder order = repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new PurchaseOrderNotFoundException(id));
        order.cancel(actorId);
        return PurchaseOrderDto.from(repository.save(order));
    }
}
