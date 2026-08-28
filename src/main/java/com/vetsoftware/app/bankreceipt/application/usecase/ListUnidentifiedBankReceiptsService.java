package com.vetsoftware.app.bankreceipt.application.usecase;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.in.ListUnidentifiedBankReceiptsUseCase;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * La bandeja del mes. El estado lo fija este servicio y no lo elige quien
 * llama: un {@code listByStatus(status)} abierto dejaria que la consola pidiera
 * {@code DISCARDED} por la ruta que la interfaz anuncia como «pendientes», y el
 * indice {@code ix_bank_receipts_inbox} dejaria de describir lo que realmente
 * se consulta.
 */
@Observed(name = "bank.receipt.list.unidentified")
@Service
public class ListUnidentifiedBankReceiptsService implements ListUnidentifiedBankReceiptsUseCase {

    private final BankReceiptRepository repository;

    public ListUnidentifiedBankReceiptsService(BankReceiptRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<BankReceiptDto> listUnidentified(int page, int pageSize) {
        return repository.findAllByStatus(BankReceiptStatus.UNIDENTIFIED, page, pageSize)
                .map(BankReceiptDto::from);
    }
}
