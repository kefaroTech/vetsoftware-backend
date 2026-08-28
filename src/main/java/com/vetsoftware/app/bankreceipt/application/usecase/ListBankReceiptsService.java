package com.vetsoftware.app.bankreceipt.application.usecase;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.in.ListBankReceiptsUseCase;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "bank.receipt.list.all")
@Service
public class ListBankReceiptsService implements ListBankReceiptsUseCase {

    private final BankReceiptRepository repository;

    public ListBankReceiptsService(BankReceiptRepository repository) {
        this.repository = repository;
    }

    /**
     * Los totales son los de la consulta y no se recalculan sobre el contenido ya
     * paginado: {@code PageResult.map} conserva los metadatos intactos.
     */
    @Override
    public PageResult<BankReceiptDto> listAll(int page, int pageSize) {
        return repository.findAll(page, pageSize).map(BankReceiptDto::from);
    }
}
