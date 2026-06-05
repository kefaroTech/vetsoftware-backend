package com.vetsoftware.app.promotion.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

public interface DeletePromotionUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('promotion.delete')")
    void execute(Long id);
}
