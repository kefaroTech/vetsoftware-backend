package com.vetsoftware.app.purchaseorder.application.usecase;

import com.vetsoftware.app.purchaseorder.application.command.SearchPurchaseOrdersCommand;
import com.vetsoftware.app.purchaseorder.application.dto.PageResult;
import com.vetsoftware.app.purchaseorder.application.dto.PurchaseOrderDto;
import com.vetsoftware.app.purchaseorder.application.port.in.SearchPurchaseOrdersUseCase;
import com.vetsoftware.app.purchaseorder.application.port.out.PurchaseOrderRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "purchaseOrder.search")
@Service
public class SearchPurchaseOrdersService implements SearchPurchaseOrdersUseCase {
    private final PurchaseOrderRepository repository;

    public SearchPurchaseOrdersService(PurchaseOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PurchaseOrderDto> execute(SearchPurchaseOrdersCommand command) {
        // readOnly tx: la paginación trae las cabeceras y el mapper hidrata las líneas LAZY aquí dentro.
        return repository.search(command).map(PurchaseOrderDto::from);
    }
}
