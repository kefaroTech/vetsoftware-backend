package com.vetsoftware.app.cashregister.application.usecase;

import com.vetsoftware.app.cashregister.application.command.SearchCashSessionsQuery;
import com.vetsoftware.app.cashregister.application.dto.CashSessionView;
import com.vetsoftware.app.cashregister.application.dto.PageResult;
import com.vetsoftware.app.cashregister.application.port.out.CashSessionRepository;
import com.vetsoftware.app.cashregister.domain.CashSession;
import com.vetsoftware.app.cashregister.domain.CashSessionStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Fake in-memory de {@link CashSessionRepository} para los tests de servicio (patrón "stubs
 * manuales" del proyecto). Almacena el agregado por id (asigna id al guardar los nuevos) y guarda
 * la referencia viva, así los servicios que hacen load → mutate → save ven el estado acumulado
 * (permite verificar idempotencia y materialización de counts).
 */
class FakeCashSessionRepository implements CashSessionRepository {

  private final Map<Long, CashSession> store = new LinkedHashMap<>();
  private long seq = 0;

  @Override
  public CashSession save(CashSession session) {
    if (session.getId() == null) session.assignId(++seq);
    store.put(session.getId(), session);
    return session;
  }

  @Override
  public Optional<CashSession> findByIdAndCompany(Long id, Long companyId) {
    return Optional.ofNullable(store.get(id))
        .filter(s -> Objects.equals(s.getCompanyId(), companyId));
  }

  @Override
  public Optional<CashSession> findOpen(Long companyId, Long branchId, String terminal) {
    return store.values().stream()
        .filter(s -> s.getStatus() == CashSessionStatus.OPEN)
        .filter(s -> Objects.equals(s.getCompanyId(), companyId))
        .filter(s -> Objects.equals(s.getBranchId(), branchId))
        .filter(s -> Objects.equals(s.getTerminal(), terminal))
        .findFirst();
  }

  @Override
  public Optional<CashSessionView> findOpenSummary(Long companyId, Long branchId, String terminal) {
    return findOpen(companyId, branchId, terminal)
        .map(
            s ->
                CashSessionView.summary(
                    s.getId(),
                    s.getBranchId(),
                    "Sede Centro",
                    s.getTerminalId(),
                    s.getTerminal(),
                    s.getStatus(),
                    s.getOpenedByEmployeeId(),
                    "Laura Gómez",
                    s.getOpenedAt(),
                    s.getOpeningFloat(),
                    CashSessionView.from(s).closingTotal(),
                    s.getClosedByEmployeeId(),
                    null,
                    s.getClosedAt(),
                    s.getNote(),
                    s.getVersion()));
  }

  @Override
  public Optional<CashSessionView> findOpenSummaryByTerminalId(
      Long companyId, Long branchId, Long terminalId) {
    return store.values().stream()
        .filter(CashSession::isOpen)
        .filter(s -> Objects.equals(s.getCompanyId(), companyId))
        .filter(s -> Objects.equals(s.getBranchId(), branchId))
        .filter(s -> Objects.equals(s.getTerminalId(), terminalId))
        .findFirst()
        .map(
            s ->
                CashSessionView.summary(
                    s.getId(),
                    s.getBranchId(),
                    "Sede Centro",
                    s.getTerminalId(),
                    s.getTerminal(),
                    s.getStatus(),
                    s.getOpenedByEmployeeId(),
                    "Laura Gómez",
                    s.getOpenedAt(),
                    s.getOpeningFloat(),
                    CashSessionView.from(s).closingTotal(),
                    s.getClosedByEmployeeId(),
                    null,
                    s.getClosedAt(),
                    s.getNote(),
                    s.getVersion()));
  }

  @Override
  public boolean existsOpen(Long companyId, Long branchId, String terminal) {
    return findOpen(companyId, branchId, terminal).isPresent();
  }

  @Override
  public boolean existsOpenByTerminalId(Long companyId, Long branchId, Long terminalId) {
    return findOpenSummaryByTerminalId(companyId, branchId, terminalId).isPresent();
  }

  @Override
  public boolean existsOpenByEmployee(Long companyId, Long employeeId) {
    return employeeId != null
        && store.values().stream()
            .anyMatch(
                s ->
                    s.isOpen()
                        && Objects.equals(s.getCompanyId(), companyId)
                        && Objects.equals(s.getOpenedByEmployeeId(), employeeId));
  }

  @Override
  public Optional<CashSession> findOpenByEmployee(Long companyId, Long employeeId) {
    if (employeeId == null) return Optional.empty();
    return store.values().stream()
        .filter(CashSession::isOpen)
        .filter(s -> Objects.equals(s.getCompanyId(), companyId))
        .filter(s -> Objects.equals(s.getOpenedByEmployeeId(), employeeId))
        .findFirst();
  }

  @Override
  public PageResult<CashSessionView> search(SearchCashSessionsQuery query) {
    List<CashSessionView> content = new ArrayList<>();
    for (CashSession s : store.values()) {
      if (!Objects.equals(s.getCompanyId(), query.companyId())) continue;
      if (query.branchId() != null && !Objects.equals(s.getBranchId(), query.branchId())) continue;
      content.add(
          CashSessionView.summary(
              s.getId(),
              s.getBranchId(),
              null,
              s.getTerminalId(),
              s.getTerminal(),
              s.getStatus(),
              s.getOpenedByEmployeeId(),
              null,
              s.getOpenedAt(),
              s.getOpeningFloat(),
              CashSessionView.from(s).closingTotal(),
              s.getClosedByEmployeeId(),
              null,
              s.getClosedAt(),
              s.getNote(),
              s.getVersion()));
    }
    return new PageResult<>(content, query.page(), query.pageSize(), content.size(), 1);
  }

  @Override
  public List<CashSessionView> findOpenSummaries(Long companyId, Set<Long> accessibleBranchIds) {
    return store.values().stream()
        .filter(s -> Objects.equals(s.getCompanyId(), companyId))
        .filter(CashSession::isOpen)
        .filter(s -> accessibleBranchIds == null || accessibleBranchIds.contains(s.getBranchId()))
        .map(CashSessionView::from)
        .toList();
  }
}
