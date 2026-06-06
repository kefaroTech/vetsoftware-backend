package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.ChangeOpenAccountStatusUseCase;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open_account.change_status")
@Service
public class ChangeOpenAccountStatusService implements ChangeOpenAccountStatusUseCase {
    private final OpenAccountRepository repository;
    private final EmployeeQueryPort employeeQueryPort;

    public ChangeOpenAccountStatusService(OpenAccountRepository repository,
                                          EmployeeQueryPort employeeQueryPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
    }

    @Override
    @Transactional
    public OpenAccountDto execute(ChangeOpenAccountStatusCommand command) {
        OpenAccount openAccount = repository.findById(command.id())
            .orElseThrow(() -> new OpenAccountNotFoundException(command.id()));
        EmployeeRef closedBy = employeeQueryPort.findById(command.employeeId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
        OpenAccountStatus newStatus = OpenAccountStatus.valueOf(command.status().toUpperCase());
        openAccount.changeStatus(newStatus, closedBy, command.reason());
        return OpenAccountDto.from(repository.save(openAccount));
    }
}
