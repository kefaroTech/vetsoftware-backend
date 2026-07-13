package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.in.ListPurchaseOrdersUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "purchaseOrder.list_by_company")
@Service
public class ListPurchaseOrdersService implements ListPurchaseOrdersUseCase {
    private final PurchaseOrderRepository repository;

    public ListPurchaseOrdersService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> listByCompany(Long companyId) {
        return repository.findAllByCompanyId(companyId).stream().map(PurchaseOrderDto::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> listDisabledByCompany(Long companyId) {
        // readOnly tx: la query nativa trae las pausadas y el mapper hidrata sus asociaciones LAZY aquí dentro.
        return repository.findAllDisabledByCompanyId(companyId).stream().map(PurchaseOrderDto::from).toList();
    }
}
