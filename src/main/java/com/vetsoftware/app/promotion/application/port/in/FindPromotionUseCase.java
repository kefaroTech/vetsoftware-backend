package com.vetsoftware.app.promotion.application.port.in;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPromotionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('promotion.read')")
    PromotionDto findById(Long id);
}
