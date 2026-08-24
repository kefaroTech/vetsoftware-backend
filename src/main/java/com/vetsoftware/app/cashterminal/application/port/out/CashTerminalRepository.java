package com.vetsoftware.app.cashterminal.application.port.out;

import com.vetsoftware.app.cashterminal.domain.CashTerminal;
import java.util.List;
import java.util.Optional;

public interface CashTerminalRepository {

    List<CashTerminal> findAllByBranch(Long companyId, Long branchId, boolean activeOnly);

    Optional<CashTerminal> findByIdAndCompanyId(Long id, Long companyId);

    boolean existsCode(Long companyId, Long branchId, String code);

    boolean existsOtherWithCode(Long companyId, Long branchId, String code, Long id);

    CashTerminal save(CashTerminal terminal);
}
