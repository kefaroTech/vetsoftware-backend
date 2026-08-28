package com.vetsoftware.app.bankreceipt.application.usecase;

import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.in.FindBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "bank.receipt.find")
@Service
public class FindBankReceiptService implements FindBankReceiptUseCase {

    private final BankReceiptRepository repository;

    public FindBankReceiptService(BankReceiptRepository repository) {
        this.repository = repository;
    }

    /**
     * La carga es ancha porque no existe otra: la tabla no tiene empresa. Lo que
     * exime a este servicio de {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} no es una
     * excepcion escrita a mano, es que el puerto de salida no declara ninguna
     * variante acotada que este servicio pudiera estar ignorando.
     */
    @Override
    public BankReceiptDto findById(Long id) {
        return repository.findById(id).map(BankReceiptDto::from)
                .orElseThrow(() -> new BankReceiptNotFoundException(id));
    }
}
