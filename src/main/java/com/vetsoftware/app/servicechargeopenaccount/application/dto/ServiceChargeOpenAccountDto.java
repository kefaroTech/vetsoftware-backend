package com.vetsoftware.app.servicechargeopenaccount.application.dto;

import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import java.time.LocalDateTime;

public record ServiceChargeOpenAccountDto(
        Long id,
        AnimalSummaryDto animal,
        ServiceSummaryDto service,
        OpenAccountSummaryDto openAccount,
        EmployeeSummaryDto createdBy,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static ServiceChargeOpenAccountDto from(ServiceChargeOpenAccount charge) {
        return new ServiceChargeOpenAccountDto(
                charge.getId(),
                AnimalSummaryDto.from(charge.getAnimal()),
                ServiceSummaryDto.from(charge.getService()),
                OpenAccountSummaryDto.from(charge.getOpenAccount()),
                charge.getCreatedBy() == null ? null : EmployeeSummaryDto.from(charge.getCreatedBy()),
                charge.getCreatedDate(),
                charge.isEnabled());
    }
}
