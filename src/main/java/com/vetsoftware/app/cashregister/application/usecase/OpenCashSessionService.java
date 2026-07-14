package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.OpenCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.in.OpenCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.application.port.out.CashTerminalQueryPort;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionAlreadyOpenException;
import com.vetsoftware.app.cashregister.domain.EmployeeCashSessionAlreadyOpenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la caja de una sede con la base inicial. Cada empleado puede mantener una sola sesión OPEN y cada terminal
 * solo una por sede; los índices únicos condicionales de la BD cubren las carreras residuales.
 */
@Service
public class OpenCashSessionService implements OpenCashSessionUseCase {

    private final CashSessionRepository repository;
    private final BranchQueryPort branchQueryPort;
    private final CashTerminalQueryPort terminalQueryPort;

    public OpenCashSessionService(CashSessionRepository repository, BranchQueryPort branchQueryPort,
                                  CashTerminalQueryPort terminalQueryPort) {
        this.repository = repository;
        this.branchQueryPort = branchQueryPort;
        this.terminalQueryPort = terminalQueryPort;
    }

    @Override
    @Transactional
    public CashSessionView open(OpenCashSessionCommand command) {
        if (!branchQueryPort.existsActiveInCompany(command.branchId(), command.companyId())) {
            throw new IllegalArgumentException("Sede no válida o inactiva: " + command.branchId());
        }
        CashTerminalQueryPort.TerminalRef terminal = terminalQueryPort
            .findActive(command.terminalId(), command.companyId(), command.branchId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Terminal no válido, inactivo o perteneciente a otra sede: " + command.terminalId()));
        if (repository.existsOpenByEmployee(command.companyId(), command.openedByEmployeeId())) {
            throw new EmployeeCashSessionAlreadyOpenException();
        }
        repository.findOpenSummary(command.companyId(), command.branchId(), terminal.code())
            .ifPresent(open -> {
                throw new CashSessionAlreadyOpenException(
                    open.branchName(), open.terminal(), open.openedByEmployeeName());
            });
        CashSession session = CashSession.open(command.companyId(), command.branchId(), terminal.code(),
            command.openedByEmployeeId(), command.openingFloat(), command.note());
        return CashSessionView.from(repository.save(session));
    }

}
