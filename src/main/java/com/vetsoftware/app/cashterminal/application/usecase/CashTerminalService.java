package com.vetsoftware.app.cashterminal.application.usecase;

import com.vetsoftware.app.cashregister.application.port.out.BranchQueryPort;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashterminal.application.dto.CashTerminalDto;
import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaEntity;
import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaRepository;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashTerminalService {
    private final CashTerminalJpaRepository repository;
    private final BranchQueryPort branchQueryPort;
    private final CashSessionRepository cashSessionRepository;

    public CashTerminalService(CashTerminalJpaRepository repository,
            BranchQueryPort branchQueryPort, CashSessionRepository cashSessionRepository) {
        this.repository = repository;
        this.branchQueryPort = branchQueryPort;
        this.cashSessionRepository = cashSessionRepository;
    }

    @Observed(name = "cash.terminal.list")
    @Transactional(readOnly = true)
    public List<CashTerminalDto> list(Long companyId, Long branchId, boolean activeOnly) {
        validateActiveBranch(companyId, branchId);
        List<CashTerminalJpaEntity> terminals = activeOnly
                ? repository.findAllByCompanyIdAndBranchIdAndActiveTrueOrderByNameAsc(companyId,
                        branchId)
                : repository.findAllByCompanyIdAndBranchIdOrderByActiveDescNameAsc(companyId,
                        branchId);
        return terminals.stream().map(CashTerminalDto::from).toList();
    }

    @Observed(name = "cash.terminal.create")
    @Transactional
    public CashTerminalDto create(Long companyId, Long branchId, String name, String code) {
        validateActiveBranch(companyId, branchId);
        String normalizedName = normalizeName(name);
        String normalizedCode = normalizeCode(code);
        if (repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCase(companyId, branchId,
                normalizedCode)) {
            throw new IllegalArgumentException("Ya existe un terminal con ese código en la sede");
        }
        CashTerminalJpaEntity entity = new CashTerminalJpaEntity();
        entity.setCompanyId(companyId);
        entity.setBranchId(branchId);
        entity.setName(normalizedName);
        entity.setCode(normalizedCode);
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.now());
        return CashTerminalDto.from(repository.save(entity));
    }

    @Observed(name = "cash.terminal.update")
    @Transactional
    public CashTerminalDto update(Long companyId, Long id, String name, String code) {
        CashTerminalJpaEntity entity = get(companyId, id);
        String normalizedName = normalizeName(name);
        String normalizedCode = normalizeCode(code);
        if (!entity.getCode().equalsIgnoreCase(normalizedCode) && cashSessionRepository
                .existsOpenByTerminalId(companyId, entity.getBranchId(), entity.getId())) {
            throw new IllegalStateException(
                    "No se puede cambiar el código de un terminal con una caja abierta");
        }
        if (repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCaseAndIdNot(companyId,
                entity.getBranchId(), normalizedCode, id)) {
            throw new IllegalArgumentException("Ya existe un terminal con ese código en la sede");
        }
        entity.setName(normalizedName);
        entity.setCode(normalizedCode);
        return CashTerminalDto.from(repository.save(entity));
    }

    @Observed(name = "cash.terminal.set.active")
    @Transactional
    public CashTerminalDto setActive(Long companyId, Long id, boolean active) {
        CashTerminalJpaEntity entity = get(companyId, id);
        if (!active && entity.isActive() && cashSessionRepository.existsOpenByTerminalId(companyId,
                entity.getBranchId(), entity.getId())) {
            throw new IllegalStateException(
                    "No se puede desactivar un terminal con una caja abierta");
        }
        entity.setActive(active);
        return CashTerminalDto.from(repository.save(entity));
    }

    private CashTerminalJpaEntity get(Long companyId, Long id) {
        return repository.findByIdAndCompanyId(id, companyId).orElseThrow(
                () -> new IllegalArgumentException("Terminal de caja no encontrado: " + id));
    }

    private void validateActiveBranch(Long companyId, Long branchId) {
        if (branchId == null || !branchQueryPort.existsActiveInCompany(branchId, companyId)) {
            throw new IllegalArgumentException("Sede no válida o inactiva: " + branchId);
        }
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        String normalized = value.trim();
        if (normalized.length() > 120)
            throw new IllegalArgumentException("El nombre supera 120 caracteres");
        return normalized;
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("El código es obligatorio");
        String normalized = value.trim().toUpperCase();
        if (normalized.length() > 60)
            throw new IllegalArgumentException("El código supera 60 caracteres");
        return normalized;
    }
}
