package com.vetsoftware.app.cashregister.infrastructure.persistence;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashMovement;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionCount;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCashSessionRepository implements CashSessionRepository {

    private final CashSessionJpaRepository jpaRepository;

    public JpaCashSessionRepository(CashSessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    // save() de una sesión nueva → persist; de una existente → merge con la versión del dominio (optimistic lock:
    // dos cierres o dos appends concurrentes hacen que el 2º falle con ObjectOptimisticLockingFailureException → 409).
    // Los movimientos/counts nuevos (id == null) se insertan; los existentes (append-only, inmutables) se re-mapean.
    @Override
    public CashSession save(CashSession session) {
        CashSessionJpaEntity entity = new CashSessionJpaEntity();
        entity.setId(session.getId());
        entity.setCompanyId(session.getCompanyId());
        entity.setBranchId(session.getBranchId());
        entity.setTerminalId(session.getTerminalId());
        entity.setTerminal(session.getTerminal());
        entity.setOpenedByEmployeeId(session.getOpenedByEmployeeId());
        entity.setOpenedAt(session.getOpenedAt());
        entity.setOpeningFloat(session.getOpeningFloat());
        entity.setStatus(session.getStatus());
        entity.setClosedByEmployeeId(session.getClosedByEmployeeId());
        entity.setClosedAt(session.getClosedAt());
        entity.setNote(session.getNote());
        entity.setVersion(session.getVersion());
        for (CashMovement m : session.getMovements()) {
            CashMovementJpaEntity me = new CashMovementJpaEntity();
            me.setId(m.getId());
            me.setType(m.getType());
            me.setMethod(m.getMethod());
            me.setAmount(m.getAmount());
            me.setReferenceType(m.getReferenceType());
            me.setReferenceId(m.getReferenceId());
            me.setCreatedByEmployeeId(m.getCreatedByEmployeeId());
            me.setCreatedAt(m.getCreatedAt());
            me.setNote(m.getNote());
            entity.addMovement(me);
        }
        for (CashSessionCount c : session.getCounts()) {
            CashSessionCountJpaEntity ce = new CashSessionCountJpaEntity();
            ce.setId(c.getId());
            ce.setMethod(c.getMethod());
            ce.setExpectedAmount(c.getExpectedAmount());
            ce.setCountedAmount(c.getCountedAmount());
            ce.setDifference(c.difference());
            entity.addCount(ce);
        }
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<CashSession> findByIdAndCompany(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompanyId(id, companyId).map(this::toDomain);
    }

    @Override
    public Optional<CashSession> findOpen(Long companyId, Long branchId, String terminal) {
        return jpaRepository.findFirstByCompanyIdAndBranchIdAndTerminalAndStatus(
            companyId, branchId, terminal, CashSessionStatus.OPEN).map(this::toDomain);
    }

    @Override
    public Optional<CashSessionView> findOpenSummary(Long companyId, Long branchId, String terminal) {
        return jpaRepository.findOpenSummary(companyId, branchId, terminal).map(this::toSummary);
    }

    @Override
    public Optional<CashSessionView> findOpenSummaryByTerminalId(Long companyId, Long branchId, Long terminalId) {
        return jpaRepository.findOpenSummaryByTerminalId(companyId, branchId, terminalId).map(this::toSummary);
    }

    @Override
    public boolean existsOpen(Long companyId, Long branchId, String terminal) {
        return jpaRepository.existsByCompanyIdAndBranchIdAndTerminalAndStatus(
            companyId, branchId, terminal, CashSessionStatus.OPEN);
    }

    @Override
    public boolean existsOpenByTerminalId(Long companyId, Long branchId, Long terminalId) {
        return jpaRepository.existsByCompanyIdAndBranchIdAndTerminalIdAndStatus(
            companyId, branchId, terminalId, CashSessionStatus.OPEN);
    }

    @Override
    public boolean existsOpenByEmployee(Long companyId, Long employeeId) {
        return employeeId != null && jpaRepository.existsByCompanyIdAndOpenedByEmployeeIdAndStatus(
            companyId, employeeId, CashSessionStatus.OPEN);
    }

    @Override
    public Optional<CashSession> findOpenByEmployee(Long companyId, Long employeeId) {
        if (employeeId == null) return Optional.empty();
        return jpaRepository.findFirstByCompanyIdAndOpenedByEmployeeIdAndStatus(
            companyId, employeeId, CashSessionStatus.OPEN).map(this::toDomain);
    }

    @Override
    public PageResult<CashSessionView> search(SearchCashSessionsQuery query) {
        LocalDateTime from = query.from() == null ? null : query.from().atStartOfDay();
        LocalDateTime to = query.to() == null ? null : query.to().plusDays(1).atStartOfDay();
        Page<CashSessionSummaryRow> page = jpaRepository.search(query.companyId(), query.branchId(),
            query.employeeId(), from, to, PageRequest.of(query.page(), query.pageSize()));
        List<CashSessionView> content = page.getContent().stream().map(this::toSummary).toList();
        return new PageResult<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
            page.getTotalPages());
    }

    @Override
    public List<CashSessionView> findOpenSummaries(Long companyId, Set<Long> accessibleBranchIds) {
        if (accessibleBranchIds != null && accessibleBranchIds.isEmpty()) return List.of();
        List<CashSessionSummaryRow> rows = accessibleBranchIds == null
            ? jpaRepository.findAllOpenByCompany(companyId)
            : jpaRepository.findAllOpenByCompanyAndBranchIdIn(companyId, accessibleBranchIds);
        return rows.stream().map(this::toSummary).toList();
    }

    private CashSessionView toSummary(CashSessionSummaryRow e) {
        return CashSessionView.summary(e.getId(), e.getBranchId(), e.getBranchName(), e.getTerminalId(), e.getTerminal(),
            CashSessionStatus.valueOf(e.getStatus()), e.getOpenedByEmployeeId(), e.getOpenedByEmployeeName(),
            e.getOpenedAt(), e.getOpeningFloat(), e.getClosingTotal(), e.getClosedByEmployeeId(),
            e.getClosedByEmployeeName(), e.getClosedAt(), e.getNote(), e.getVersion());
    }

    private CashSession toDomain(CashSessionJpaEntity e) {
        List<CashMovement> movements = e.getMovements().stream()
            .map(m -> new CashMovement(m.getId(), m.getType(), m.getMethod(), m.getAmount(), m.getReferenceType(),
                m.getReferenceId(), m.getCreatedByEmployeeId(), m.getCreatedAt(), m.getNote()))
            .toList();
        List<CashSessionCount> counts = e.getCounts().stream()
            .map(c -> new CashSessionCount(c.getId(), c.getMethod(), c.getExpectedAmount(), c.getCountedAmount()))
            .toList();
        return new CashSession(e.getId(), e.getCompanyId(), e.getBranchId(), e.getTerminalId(), e.getTerminal(),
            e.getOpenedByEmployeeId(), e.getOpenedAt(), e.getOpeningFloat(), e.getStatus(),
            e.getClosedByEmployeeId(), e.getClosedAt(), e.getNote(), e.getVersion(), movements, counts);
    }
}
