package com.vetsoftware.app.customercredit.application.dto;

import com.vetsoftware.app.customercredit.domain.CustomerCreditBalance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerCreditBalanceDto(Long id, Long companyId, BigDecimal balanceAmount,
        LocalDate nextExpiryOn, LocalDateTime recalculatedAt, Long version) {

    public static CustomerCreditBalanceDto from(CustomerCreditBalance balance) {
        return new CustomerCreditBalanceDto(balance.getId(), balance.getCompanyId(),
                balance.getBalanceAmount(), balance.getNextExpiryOn(), balance.getRecalculatedAt(),
                balance.getVersion());
    }
}
