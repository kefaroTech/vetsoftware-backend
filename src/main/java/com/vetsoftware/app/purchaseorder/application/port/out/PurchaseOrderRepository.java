package com.vetsoftware.app.purchaseorder.application.port.out;

import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PageResult;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository {
    PurchaseOrder save(PurchaseOrder purchaseOrder);

    Optional<PurchaseOrder> findById(Long id);

    Optional<PurchaseOrder> findByIdAndCompanyId(Long id, Long companyId);

    List<PurchaseOrder> findAllByCompanyId(Long companyId);

    /**
     * Órdenes PAUSADAS (enabled=false) de la empresa, para el listado de
     * reactivación.
     */
    List<PurchaseOrder> findAllDisabledByCompanyId(Long companyId);

    PageResult<PurchaseOrder> search(SearchPurchaseOrdersCommand command);

    void delete(Long id);

    int reactivate(Long id, Long companyId);
}
