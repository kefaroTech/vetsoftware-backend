package com.vetsoftware.app.promotion.application.port.in;

import com.vetsoftware.app.promotion.application.command.CreatePromotionCommand;
import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreatePromotionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('promotion.create') and @authz.isMyCompany(#command.companyId))")
    PromotionDto execute(CreatePromotionCommand command);
}
