package com.vetsoftware.app.customercredit.application.usecase;

import com.vetsoftware.app.customercredit.application.command.GrantCustomerCreditCommand;
import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.customercredit.application.port.in.GrantCustomerCreditUseCase;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditEntryRepository;
import com.vetsoftware.app.customercredit.domain.CustomerCreditEntry;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abona saldo a favor abriendo un lote.
 *
 * <p>
 * <strong>Idempotente antes de insertar.</strong> {@code uq_cce_idempotency}
 * convierte el duplicado en un error de integridad, pero un 500 en la cara de
 * quien reintenta no es una respuesta idempotente: la busqueda previa, dentro
 * de la transaccion, devuelve el asiento que ya se escribio.
 *
 * <p>
 * <strong>La fila resumen nace escribiendo su cero.</strong> La columna del
 * importe no lleva valor por defecto en el changeset 324 a proposito —un
 * importe con defecto cero convierte un fallo de calculo en un dato plausible—,
 * asi que abrirla es parte del abono y no un efecto colateral.
 */
@Observed(name = "customer.credit.grant")
@Service
public class GrantCustomerCreditService implements GrantCustomerCreditUseCase {

    private final CustomerCreditEntryRepository entryRepository;
    private final CustomerCreditBalanceRepository balanceRepository;
    private final Clock clock;

    public GrantCustomerCreditService(CustomerCreditEntryRepository entryRepository,
            CustomerCreditBalanceRepository balanceRepository, Clock clock) {
        this.entryRepository = entryRepository;
        this.balanceRepository = balanceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CustomerCreditEntryDto execute(GrantCustomerCreditCommand command) {
        Optional<CustomerCreditEntry> already = entryRepository
                .findByCompanyIdAndClientRequestId(command.companyId(), command.clientRequestId());
        if (already.isPresent())
            return CustomerCreditEntryDto.from(already.get());

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = LocalDate.now(clock);

        // Antes de mover el saldo hay que tener donde moverlo. Idempotente: dos
        // abonos simultaneos de la misma empresa no pueden reventar contra
        // uq_ccb_company.
        balanceRepository.openIfAbsent(command.companyId(), now);

        CustomerCreditEntry entry = CustomerCreditEntry.grant(command.companyId(), command.amount(),
                command.originKind(), command.originPaymentId(), command.originDocumentId(),
                command.originSubscriptionId(), now, today, command.expiresOn(),
                command.clientRequestId(), now);
        CustomerCreditEntry saved = entryRepository.save(entry);

        // Un delta positivo siempre satisface la condicion del WHERE, asi que cero
        // filas aqui solo puede significar que la fila resumen no existe — y
        // acabamos de abrirla. Es un fallo de invariante, no un caso de negocio.
        int moved = balanceRepository.applyDelta(command.companyId(), saved.getAmount(), now);
        if (moved == 0)
            throw new IllegalStateException(
                    "credit balance row is missing for company " + command.companyId());

        CustomerCreditBalances.refreshNextExpiry(entryRepository, balanceRepository,
                command.companyId(), now);
        return CustomerCreditEntryDto.from(saved);
    }
}
