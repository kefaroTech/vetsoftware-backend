package com.vetsoftware.app.promotion.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeletePromotionUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('promotion.delete')")
    void execute(Long id);
}
