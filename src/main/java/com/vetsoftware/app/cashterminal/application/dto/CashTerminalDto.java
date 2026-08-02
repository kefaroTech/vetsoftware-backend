package com.vetsoftware.app.cashterminal.application.dto;

import com.vetsoftware.app.cashterminal.infrastructure.persistence.CashTerminalJpaEntity;
import java.time.LocalDateTime;

public record CashTerminalDto(Long id, Long branchId, String name, String code, boolean active,
        LocalDateTime createdAt) {
    public static CashTerminalDto from(CashTerminalJpaEntity entity) {
        return new CashTerminalDto(entity.getId(), entity.getBranchId(), entity.getName(),
                entity.getCode(), entity.isActive(), entity.getCreatedAt());
    }
}
