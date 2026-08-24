package com.vetsoftware.app.cashterminal.testsupport;

import com.vetsoftware.app.cashterminal.domain.CashTerminal;
import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaEntity;
import java.time.LocalDateTime;

public final class CashTerminalMother {

    private CashTerminalMother() {
    }

    public static CashTerminalJpaEntity entityActiva(Long id, Long companyId, Long branchId,
            String name, String code) {
        CashTerminalJpaEntity entity = new CashTerminalJpaEntity();
        entity.setId(id);
        entity.setCompanyId(companyId);
        entity.setBranchId(branchId);
        entity.setName(name);
        entity.setCode(code);
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        return entity;
    }

    public static CashTerminalJpaEntity entityInactiva(Long id, Long companyId, Long branchId,
            String name, String code) {
        CashTerminalJpaEntity entity = entityActiva(id, companyId, branchId, name, code);
        entity.setActive(false);
        return entity;
    }

    public static CashTerminal activa(Long id, Long companyId, Long branchId, String name,
            String code) {
        return new CashTerminal(id, companyId, branchId, name, code, true,
                LocalDateTime.of(2026, 1, 1, 8, 0), 0L);
    }

    public static CashTerminal inactiva(Long id, Long companyId, Long branchId, String name,
            String code) {
        return new CashTerminal(id, companyId, branchId, name, code, false,
                LocalDateTime.of(2026, 1, 1, 8, 0), 0L);
    }
}
