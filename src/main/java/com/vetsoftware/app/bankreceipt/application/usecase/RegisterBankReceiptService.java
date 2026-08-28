package com.vetsoftware.app.bankreceipt.application.usecase;

import com.vetsoftware.app.bankreceipt.application.command.RegisterBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.in.RegisterBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptAlreadyRegisteredException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga una linea del extracto.
 *
 * <p>
 * <strong>La comprobacion previa de duplicado no sustituye a la unicidad de la
 * base: la traduce.</strong> {@code uq_bank_receipts_reference} sigue siendo lo
 * unico que garantiza que no entren dos, porque entre el {@code exists} y el
 * {@code insert} cabe otra transaccion. Lo que esta comprobacion aporta es que
 * el caso comun —el operario que vuelve a cargar el fichero del mes que ya
 * cargo— conteste un 409 con la referencia y la fecha en el mensaje en vez de
 * un 500 con un {@code Duplicate entry} del driver.
 */
@Observed(name = "bank.receipt.register")
@Service
public class RegisterBankReceiptService implements RegisterBankReceiptUseCase {

    private final BankReceiptRepository repository;
    private final Clock clock;

    public RegisterBankReceiptService(BankReceiptRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BankReceiptDto execute(RegisterBankReceiptCommand command) {
        if (repository.existsByBankReferenceAndReceivedOn(command.bankReference(),
                command.receivedOn()))
            throw new BankReceiptAlreadyRegisteredException(command.bankReference(),
                    command.receivedOn());

        BankReceipt receipt = BankReceipt.register(command.bankAccountRef(),
                command.bankReference(), command.receivedOn(), command.amount(),
                command.description(), LocalDateTime.now(clock));
        return BankReceiptDto.from(repository.save(receipt));
    }
}
