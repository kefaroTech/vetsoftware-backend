package com.vetsoftware.app.withholdingconfig.application.dto;

import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithholdingConfigDto(
        Long id,
        Long companyId,
        BigDecimal reteFuenteRate,
        BigDecimal reteIvaRate,
        BigDecimal reteIcaRate,
        LocalDateTime createdDate,
        boolean enabled
) {
    public static WithholdingConfigDto from(WithholdingConfig c) {
        return new WithholdingConfigDto(c.getId(),
                c.getCompany() == null ? null : c.getCompany().id(),
                c.getReteFuenteRate(), c.getReteIvaRate(), c.getReteIcaRate(),
                c.getCreatedDate(), c.isEnabled());
    }
}
