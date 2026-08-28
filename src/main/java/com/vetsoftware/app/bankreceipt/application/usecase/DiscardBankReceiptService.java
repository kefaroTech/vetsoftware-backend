package com.vetsoftware.app.bankreceipt.application.usecase;

import com.vetsoftware.app.bankreceipt.application.command.DiscardBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.in.DiscardBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Archiva una entrada que no corresponde a ningun cliente.
 *
 * <p>
 * <strong>Es un service aparte del que identifica aunque el codigo se
 * parezca.</strong> Son dos decisiones distintas de negocio —«ya se de quien
 * es» y «esto no es de nadie»— y el CLAUDE.md pide un service por caso de uso
 * justamente para que la segunda no herede en silencio los cambios de la
 * primera el dia que identificar empiece a exigir el dueño.
 */
@Observed(name = "bank.receipt.discard")
@Service
public class DiscardBankReceiptService implements DiscardBankReceiptUseCase {

    private final BankReceiptRepository repository;
    private final Clock clock;

    public DiscardBankReceiptService(BankReceiptRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BankReceiptDto execute(DiscardBankReceiptCommand command) {
        BankReceipt receipt = repository.findById(command.id())
                .orElseThrow(() -> new BankReceiptNotFoundException(command.id()));
        receipt.discard(LocalDateTime.now(clock));
        return BankReceiptDto.from(repository.save(receipt));
    }
}
