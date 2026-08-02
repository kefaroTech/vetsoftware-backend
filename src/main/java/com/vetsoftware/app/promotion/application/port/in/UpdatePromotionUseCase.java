package com.vetsoftware.app.promotion.application.port.in;

import com.vetsoftware.app.promotion.application.command.UpdatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface UpdatePromotionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('promotion.update') and"
            + " @authz.isMyCompany(#command.companyId))")
    PromotionDto execute(UpdatePromotionCommand command);
}
