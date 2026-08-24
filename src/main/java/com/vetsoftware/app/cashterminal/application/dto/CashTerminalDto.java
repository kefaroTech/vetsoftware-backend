package com.vetsoftware.app.cashterminal.application.dto;

import com.vetsoftware.app.cashterminal.domain.CashTerminal;
import java.time.LocalDateTime;

public record CashTerminalDto(Long id, Long branchId, String name, String code, boolean active,
        LocalDateTime createdAt) {
    public static CashTerminalDto from(CashTerminal terminal) {
        return new CashTerminalDto(terminal.getId(), terminal.getBranchId(), terminal.getName(),
                terminal.getCode(), terminal.isActive(), terminal.getCreatedAt());
    }
}
