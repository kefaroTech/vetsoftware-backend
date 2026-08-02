package com.vetsoftware.app.promotion.application.port.in;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPromotionUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('promotion.read') and @authz.isMyCompany(#companyId))")
  PromotionDto findById(Long id, Long companyId);
}
