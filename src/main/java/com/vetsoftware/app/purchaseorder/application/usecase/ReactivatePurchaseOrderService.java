package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.in.ReactivatePurchaseOrderUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrderNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "purchase.order.reactivate")
@Service
public class ReactivatePurchaseOrderService implements ReactivatePurchaseOrderUseCase {
  private final PurchaseOrderRepository repository;

  public ReactivatePurchaseOrderService(PurchaseOrderRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public PurchaseOrderDto execute(Long id, Long companyId) {
    int rows = repository.reactivate(id, companyId);
    if (rows == 0) throw new PurchaseOrderNotFoundException(id);
    return PurchaseOrderDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new PurchaseOrderNotFoundException(id)));
  }
}
