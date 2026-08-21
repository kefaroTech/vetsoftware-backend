package com.vetsoftware.app.purchaseorder.application.port.out;

import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.purchaseorder.domain.PurchaseOrder;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository {
    PurchaseOrder save(PurchaseOrder purchaseOrder);

    Optional<PurchaseOrder> findByIdAndCompanyId(Long id, Long companyId);

    List<PurchaseOrder> findAllByCompanyId(Long companyId);

    PageResult<PurchaseOrder> search(SearchPurchaseOrdersCommand command);

    /**
     * Pausa (baja lógica) acotada al tenant. El {@code companyId} viaja hasta el
     * {@code WHERE} del UPDATE: la lectura previa del caso de uso valida la
     * propiedad, pero el filtro del SQL es lo que la sostiene si alguien reordena
     * el servicio o llama al adaptador desde otro sitio.
     */
    void delete(Long id, Long companyId);
}
