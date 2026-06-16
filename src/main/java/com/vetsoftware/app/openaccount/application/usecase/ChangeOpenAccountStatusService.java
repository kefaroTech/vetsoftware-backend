package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.event.OpenAccountClosedForEmissionEvent;
import com.vetsoftware.app.openaccount.application.port.in.ChangeOpenAccountStatusUseCase;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open_account.change_status")
@Service
public class ChangeOpenAccountStatusService implements ChangeOpenAccountStatusUseCase {
    private static final String DEFAULT_DOCUMENT_TYPE = "DOC_EQUIV_POS";

    private final OpenAccountRepository repository;
    private final EmployeeQueryPort employeeQueryPort;
    private final ApplicationEventPublisher events;

    public ChangeOpenAccountStatusService(OpenAccountRepository repository,
                                          EmployeeQueryPort employeeQueryPort,
                                          ApplicationEventPublisher events) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
        this.events = events;
    }

    @Override
    @Transactional
    public OpenAccountDto execute(ChangeOpenAccountStatusCommand command) {
        OpenAccount openAccount = repository.findById(command.id())
            .orElseThrow(() -> new OpenAccountNotFoundException(command.id()));
        if (!openAccount.getCompany().id().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        EmployeeRef closedBy = employeeQueryPort.findById(command.employeeId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
        OpenAccountStatus newStatus = OpenAccountStatus.valueOf(command.status().toUpperCase());
        openAccount.changeStatus(newStatus, closedBy, command.reason());
        OpenAccount saved = repository.save(openAccount);

        // Cobro/cierre de la venta: dispara la auto-emisión del documento electrónico. Se publica un evento
        // que un listener de electronicdocument procesa TRAS el commit (best-effort): así la emisión ve la
        // cuenta ya CLOSE y un fallo (DIAN caída, sin numeración) nunca revierte ni bloquea el cierre.
        if (newStatus == OpenAccountStatus.CLOSE) {
            String documentType = command.documentType() != null && !command.documentType().isBlank()
                ? command.documentType() : DEFAULT_DOCUMENT_TYPE;
            events.publishEvent(new OpenAccountClosedForEmissionEvent(
                saved.getId(), saved.getCompany().id(), documentType, command.finalConsumer()));
        }
        return OpenAccountDto.from(saved);
    }
}
