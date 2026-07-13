package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.OpenCashSessionCommand;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.port.in.OpenCashSessionUseCase;
import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionAlreadyOpenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la caja de una sede con la base inicial. Valida que la sede esté activa y que no exista ya una sesión OPEN
 * para (empresa, sede, terminal); el índice único condicional de la BD cubre la carrera residual.
 */
@Service
public class OpenCashSessionService implements OpenCashSessionUseCase {

    private final CashSessionRepository repository;
    private final BranchQueryPort branchQueryPort;

    public OpenCashSessionService(CashSessionRepository repository, BranchQueryPort branchQueryPort) {
        this.repository = repository;
        this.branchQueryPort = branchQueryPort;
    }

    @Override
    @Transactional
    public CashSessionView open(OpenCashSessionCommand command) {
        if (!branchQueryPort.existsActiveInCompany(command.branchId(), command.companyId())) {
            throw new IllegalArgumentException("Sede no válida o inactiva: " + command.branchId());
        }
        String terminal = resolveTerminal(command.terminal());
        if (repository.existsOpen(command.companyId(), command.branchId(), terminal)) {
            throw new CashSessionAlreadyOpenException(command.branchId(), terminal);
        }
        CashSession session = CashSession.open(command.companyId(), command.branchId(), terminal,
            command.openedByEmployeeId(), command.openingFloat(), command.note());
        return CashSessionView.from(repository.save(session));
    }

    private static String resolveTerminal(String terminal) {
        return (terminal == null || terminal.isBlank()) ? CashSession.DEFAULT_TERMINAL : terminal.trim();
    }
}
