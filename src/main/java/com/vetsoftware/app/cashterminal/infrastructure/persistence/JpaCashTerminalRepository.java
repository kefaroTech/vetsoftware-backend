package com.vetsoftware.app.cashterminal.infrastructure.persistence;

import com.vetsoftware.app.cashterminal.application.port.out.CashTerminalRepository;
import com.vetsoftware.app.cashterminal.domain.CashTerminal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCashTerminalRepository implements CashTerminalRepository {

    private final CashTerminalJpaRepository repository;

    public JpaCashTerminalRepository(CashTerminalJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CashTerminal> findAllByBranch(Long companyId, Long branchId, boolean activeOnly) {
        List<CashTerminalJpaEntity> entities = activeOnly
                ? repository.findAllByCompanyIdAndBranchIdAndActiveTrueOrderByNameAsc(companyId,
                        branchId)
                : repository.findAllByCompanyIdAndBranchIdOrderByActiveDescNameAsc(companyId,
                        branchId);
        return entities.stream().map(JpaCashTerminalRepository::toDomain).toList();
    }

    @Override
    public Optional<CashTerminal> findByIdAndCompanyId(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId)
                .map(JpaCashTerminalRepository::toDomain);
    }

    @Override
    public boolean existsCode(Long companyId, Long branchId, String code) {
        return repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCase(companyId, branchId, code);
    }

    @Override
    public boolean existsOtherWithCode(Long companyId, Long branchId, String code, Long id) {
        return repository.existsByCompanyIdAndBranchIdAndCodeIgnoreCaseAndIdNot(companyId, branchId,
                code, id);
    }

    @Override
    public CashTerminal save(CashTerminal terminal) {
        return toDomain(repository.save(toJpa(terminal)));
    }

    private static CashTerminal toDomain(CashTerminalJpaEntity entity) {
        return new CashTerminal(entity.getId(), entity.getCompanyId(), entity.getBranchId(),
                entity.getName(), entity.getCode(), entity.isActive(), entity.getCreatedAt(),
                entity.getVersion());
    }

    private static CashTerminalJpaEntity toJpa(CashTerminal terminal) {
        CashTerminalJpaEntity entity = new CashTerminalJpaEntity();
        entity.setId(terminal.getId());
        entity.setCompanyId(terminal.getCompanyId());
        entity.setBranchId(terminal.getBranchId());
        entity.setName(terminal.getName());
        entity.setCode(terminal.getCode());
        entity.setActive(terminal.isActive());
        entity.setCreatedAt(terminal.getCreatedAt());
        entity.setVersion(terminal.getVersion());
        return entity;
    }
}
