package com.vetsoftware.app.promotion.application.port.in;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ReactivatePromotionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('promotion.delete')")
    PromotionDto execute(Long id);
}
