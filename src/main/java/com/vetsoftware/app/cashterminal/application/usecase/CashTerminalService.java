package com.vetsoftware.app.cashterminal.application.usecase;

import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashterminal.application.dto.CashTerminalDto;
import com.vetsoftware.app.cashterminal.application.port.out.CashTerminalCapacityPort;
import com.vetsoftware.app.cashterminal.application.port.out.CashTerminalRepository;
import com.vetsoftware.app.cashterminal.domain.CashTerminal;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashTerminalService {
    private final CashTerminalRepository repository;
    private final BranchQueryPort branchQueryPort;
    private final CashSessionRepository cashSessionRepository;
    private final CashTerminalCapacityPort cashTerminalCapacityPort;

    public CashTerminalService(CashTerminalRepository repository, BranchQueryPort branchQueryPort,
            CashSessionRepository cashSessionRepository,
            CashTerminalCapacityPort cashTerminalCapacityPort) {
        this.repository = repository;
        this.branchQueryPort = branchQueryPort;
        this.cashSessionRepository = cashSessionRepository;
        this.cashTerminalCapacityPort = cashTerminalCapacityPort;
    }

    @Observed(name = "cash.terminal.list")
    @Transactional(readOnly = true)
    public List<CashTerminalDto> list(Long companyId, Long branchId, boolean activeOnly) {
        validateActiveBranch(companyId, branchId);
        List<CashTerminal> terminals = repository.findAllByBranch(companyId, branchId, activeOnly);
        return terminals.stream().map(CashTerminalDto::from).toList();
    }

    @Observed(name = "cash.terminal.create")
    @Transactional
    public CashTerminalDto create(Long companyId, Long branchId, String name, String code) {
        validateActiveBranch(companyId, branchId);
        CashTerminal terminal = CashTerminal.create(companyId, branchId, name, code,
                LocalDateTime.now());
        if (repository.existsCode(companyId, branchId, terminal.getCode())) {
            throw new IllegalArgumentException("Ya existe un terminal con ese código en la sede");
        }
        cashTerminalCapacityPort.reserve(companyId);
        return CashTerminalDto.from(repository.save(terminal));
    }

    @Observed(name = "cash.terminal.update")
    @Transactional
    public CashTerminalDto update(Long companyId, Long id, String name, String code) {
        CashTerminal terminal = get(companyId, id);
        CashTerminal normalized = CashTerminal.create(companyId, terminal.getBranchId(), name, code,
                terminal.getCreatedAt());
        if (!terminal.getCode().equalsIgnoreCase(normalized.getCode()) && cashSessionRepository
                .existsOpenByTerminalId(companyId, terminal.getBranchId(), terminal.getId())) {
            throw new IllegalStateException(
                    "No se puede cambiar el código de un terminal con una caja abierta");
        }
        if (repository.existsOtherWithCode(companyId, terminal.getBranchId(), normalized.getCode(),
                id)) {
            throw new IllegalArgumentException("Ya existe un terminal con ese código en la sede");
        }
        terminal.rename(name, code);
        return CashTerminalDto.from(repository.save(terminal));
    }

    @Observed(name = "cash.terminal.set.active")
    @Transactional
    public CashTerminalDto setActive(Long companyId, Long id, boolean active) {
        CashTerminal terminal = get(companyId, id);
        if (terminal.isActive() == active) {
            return CashTerminalDto.from(terminal);
        }
        if (!active && terminal.isActive() && cashSessionRepository
                .existsOpenByTerminalId(companyId, terminal.getBranchId(), terminal.getId())) {
            throw new IllegalStateException(
                    "No se puede desactivar un terminal con una caja abierta");
        }
        if (active) {
            cashTerminalCapacityPort.reserve(companyId);
        }
        terminal.setActive(active);
        CashTerminal saved = repository.save(terminal);
        if (!active) {
            cashTerminalCapacityPort.release(companyId);
        }
        return CashTerminalDto.from(saved);
    }

    private CashTerminal get(Long companyId, Long id) {
        return repository.findByIdAndCompanyId(id, companyId).orElseThrow(
                () -> new IllegalArgumentException("Terminal de caja no encontrado: " + id));
    }

    private void validateActiveBranch(Long companyId, Long branchId) {
        if (branchId == null || !branchQueryPort.existsActiveInCompany(branchId, companyId)) {
            throw new IllegalArgumentException("Sede no válida o inactiva: " + branchId);
        }
    }

}
