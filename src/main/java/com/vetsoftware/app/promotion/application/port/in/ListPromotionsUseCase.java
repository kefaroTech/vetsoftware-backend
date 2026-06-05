package com.vetsoftware.app.promotion.application.port.in;

import com.vetsoftware.app.promotion.application.dto.PromotionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPromotionsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or @authz.isMyCompany(#companyId)")
    List<PromotionDto> listByCompany(Long companyId);
}
