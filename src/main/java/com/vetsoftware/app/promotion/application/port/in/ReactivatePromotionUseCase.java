package com.vetsoftware.app.promotion.application.port.in;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivatePromotionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('promotion.delete')"
            + " and @authz.isMyCompany(#companyId))")
    PromotionDto execute(Long id, Long companyId);
}
