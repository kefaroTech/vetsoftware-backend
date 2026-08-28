package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.ResolveExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.in.ResolveExternalInvoiceReconciliationUseCase;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliation;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra el expediente con firma, nota y periodo contable.
 *
 * <p>
 * <strong>El instante lo pone el reloj inyectado, nunca el cliente.</strong>
 * {@code resolvedAt} es la mitad del par que decide en que cierre queda el
 * ajuste -la otra mitad es {@code postingPeriod}-, y una fecha que escribe
 * quien resuelve se puede antedatar a un periodo ya cerrado. Por eso el
 * {@code Clock} se inyecta por constructor y no hay ningun
 * {@code LocalDateTime.now()} pelado.
 *
 * <p>
 * <strong>La firma se comprueba que exista.</strong> Sin ese paso,
 * {@code fk_eir_resolved_by} rechazaria la escritura como un error de
 * integridad al final de la transaccion, cuando ya no se puede decir quien
 * falta.
 */
@Observed(name = "external.invoice.reconciliation.resolve")
@Service
public class ResolveExternalInvoiceReconciliationService
        implements
            ResolveExternalInvoiceReconciliationUseCase {

    private final ExternalInvoiceReconciliationRepository repository;
    private final SystemUserValidationPort systemUserValidationPort;
    private final Clock clock;

    public ResolveExternalInvoiceReconciliationService(
            ExternalInvoiceReconciliationRepository repository,
            SystemUserValidationPort systemUserValidationPort, Clock clock) {
        this.repository = repository;
        this.systemUserValidationPort = systemUserValidationPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ExternalInvoiceReconciliationDto execute(
            ResolveExternalInvoiceReconciliationCommand command) {
        ExternalInvoiceReconciliation reconciliation = repository.findById(command.id())
                .orElseThrow(
                        () -> new ExternalInvoiceReconciliationNotFoundException(command.id()));
        if (!systemUserValidationPort.existsById(command.resolvedBySystemUserId()))
            throw new IllegalArgumentException(
                    "System user not found: " + command.resolvedBySystemUserId());

        reconciliation.resolve(command.resolvedBySystemUserId(), command.resolutionNote(),
                command.postingPeriod(), LocalDateTime.now(clock));
        return ExternalInvoiceReconciliationDto.from(repository.save(reconciliation));
    }
}
