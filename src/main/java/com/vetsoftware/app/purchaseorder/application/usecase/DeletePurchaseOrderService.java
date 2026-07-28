package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.port.in.DeletePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "purchase.order.delete")
@Service
public class DeletePurchaseOrderService implements DeletePurchaseOrderUseCase {
    private final PurchaseOrderRepository repository;

    public DeletePurchaseOrderService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId).orElseThrow(() -> new PurchaseOrderNotFoundException(id));
        repository.delete(id);
    }
}
