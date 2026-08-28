package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.command.ExpireCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.ExpireCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CreditLot;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caduca el remanente de los lotes vencidos de una empresa.
 *
 * <p>
 * Mismo orden que el consumo y por el mismo motivo: <strong>primero se mueve el
 * saldo con la condicion dentro de la instruccion</strong> —que ademas toma el
 * bloqueo y deja fuera a cualquier consumo simultaneo— y despues se escriben
 * los asientos. Al reves, un consumo concurrente podria gastar saldo que esta
 * caducando.
 *
 * <p>
 * <strong>La fecha la pone el reloj, nunca el cliente.</strong> Si el corte
 * fuera un parametro, se podria caducar saldo todavia vivo o dejar vivo el que
 * ya no lo esta, y las dos cosas mueven dinero de alguien.
 *
 * <p>
 * <strong>Idempotente por dia.</strong> La llave de cada asiento se compone de
 * la fecha valor y el lote, asi que repetir el barrido el mismo dia encuentra
 * la fila ya escrita en vez de caducar dos veces. Los lotes ya agotados no
 * vuelven a salir de {@code findExpiredLotsByCompanyId} porque su remanente es
 * cero.
 */
@Observed(name = "customer.credit.expire")
@Service
public class ExpireCustomerCreditService implements ExpireCustomerCreditUseCase {

    /** Prefijo de la llave de idempotencia del barrido de caducidad. */
    private static final String EXPIRY_KEY_PREFIX = "EXPIRY";

    private final CustomerCreditEntryRepository entryRepository;
    private final CustomerCreditBalanceRepository balanceRepository;
    private final Clock clock;

    public ExpireCustomerCreditService(CustomerCreditEntryRepository entryRepository,
            CustomerCreditBalanceRepository balanceRepository, Clock clock) {
        this.entryRepository = entryRepository;
        this.balanceRepository = balanceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<CustomerCreditEntryDto> execute(ExpireCustomerCreditCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);

        List<CreditLot> expired = entryRepository.findExpiredLotsByCompanyId(command.companyId(),
                today);
        if (expired.isEmpty())
            return List.of();

        BigDecimal total = expired.stream().map(CreditLot::remaining).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        // Cero filas aqui significa que la proyeccion dice que no hay saldo mientras
        // el libro tiene lotes vivos: han divergido. Manda el libro, asi que se
        // aborta y la fila resumen se rehace fuera de este camino.
        int moved = balanceRepository.applyDelta(command.companyId(), total.negate(), now);
        if (moved == 0)
            throw new IllegalStateException(
                    "credit balance diverged from the ledger for company " + command.companyId());

        List<CustomerCreditEntry> written = expired.stream()
                .map(lot -> entryRepository
                        .save(CustomerCreditEntry.expiration(command.companyId(), lot.remaining(),
                                lot.entryId(), now, today, expiryKey(today, lot.entryId()), now)))
                .toList();

        CustomerCreditBalances.refreshNextExpiry(entryRepository, balanceRepository,
                command.companyId(), now);
        return written.stream().map(CustomerCreditEntryDto::from).toList();
    }

    /**
     * Determinista y unica por empresa: la respalda
     * {@code uq_cce_idempotency (company_id, client_request_id)}. Lleva la fecha
     * porque el barrido corre a diario, y el lote porque caducan varios a la vez.
     */
    private static String expiryKey(LocalDate valueDate, Long lotEntryId) {
        return EXPIRY_KEY_PREFIX + CustomerCreditEntryRepository.OPERATION_SEPARATOR + valueDate
                + CustomerCreditEntryRepository.OPERATION_SEPARATOR + lotEntryId;
    }
}
