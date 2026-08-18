package com.vetsoftware.app.cashterminal.testsupport;

import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaEntity;
import java.time.LocalDateTime;

public final class CashTerminalMother {

    private CashTerminalMother() {
    }

    public static CashTerminalJpaEntity activa(Long id, Long companyId, Long branchId, String name,
            String code) {
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

    public static CashTerminalJpaEntity inactiva(Long id, Long companyId, Long branchId,
            String name, String code) {
        CashTerminalJpaEntity entity = activa(id, companyId, branchId, name, code);
        entity.setActive(false);
        return entity;
    }
}
