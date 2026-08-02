package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.in.FindPurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "purchase.order.find")
@Service
public class FindPurchaseOrderService implements FindPurchaseOrderUseCase {
    private final PurchaseOrderRepository repository;

    public FindPurchaseOrderService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDto findById(Long id, Long companyId) {
        return PurchaseOrderDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new PurchaseOrderNotFoundException(id)));
    }
}
