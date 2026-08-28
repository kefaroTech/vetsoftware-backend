package com.vetsoftware.app.bankreceipt.application.usecase;

import com.vetsoftware.app.bankreceipt.application.command.IdentifyBankReceiptCommand;
import com.vetsoftware.app.bankreceipt.application.dto.BankReceiptDto;
import com.vetsoftware.app.bankreceipt.application.port.in.IdentifyBankReceiptUseCase;
import com.vetsoftware.app.bankreceipt.application.port.out.BankReceiptRepository;
import com.vetsoftware.app.bankreceipt.domain.BankReceipt;
import com.vetsoftware.app.bankreceipt.domain.BankReceiptNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saca una entrada de la bandeja.
 *
 * <p>
 * <strong>La hora sale del reloj inyectado, nunca de un
 * {@code LocalDateTime.now()} pelado.</strong> No es solo determinismo de test:
 * {@code ClockConfig} fija la zona del negocio en {@code America/Bogota} y la
 * JVM de produccion corre en UTC. Una entrada resuelta a las 19:30 del ultimo
 * dia del mes quedaria sellada con la fecha del mes siguiente, y el trabajo
 * pendiente de un mes se contaria en el otro.
 *
 * <p>
 * <strong>{@code @Transactional} porque son dos operaciones</strong> —cargar y
 * guardar— y porque entre ellas vive la comprobacion de estado: sin la
 * transaccion, el {@code @Version} de la entidad no tendria donde comparar y el
 * empate exacto de dos operarios se resolveria por el ultimo que escribe.
 */
@Observed(name = "bank.receipt.identify")
@Service
public class IdentifyBankReceiptService implements IdentifyBankReceiptUseCase {

    private final BankReceiptRepository repository;
    private final Clock clock;

    public IdentifyBankReceiptService(BankReceiptRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BankReceiptDto execute(IdentifyBankReceiptCommand command) {
        BankReceipt receipt = repository.findById(command.id())
                .orElseThrow(() -> new BankReceiptNotFoundException(command.id()));
        receipt.identify(LocalDateTime.now(clock));
        return BankReceiptDto.from(repository.save(receipt));
    }
}
