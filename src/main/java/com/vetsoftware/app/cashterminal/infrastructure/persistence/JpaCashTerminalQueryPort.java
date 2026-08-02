package com.vetsoftware.app.cashterminal.infrastructure.persistence;

import com.vetsoftware.app.cashregister.application.port.out.CashTerminalQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaCashTerminalQueryPort implements CashTerminalQueryPort {
  private final CashTerminalJpaRepository repository;

  public JpaCashTerminalQueryPort(CashTerminalJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<TerminalRef> findActive(Long terminalId, Long companyId, Long branchId) {
    if (terminalId == null) return Optional.empty();
    return repository
        .findByIdAndCompanyIdAndBranchIdAndActiveTrue(terminalId, companyId, branchId)
        .map(t -> new TerminalRef(t.getId(), t.getName(), t.getCode()));
  }
}
