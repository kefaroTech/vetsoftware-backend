package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindPriceListUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PriceListDto findById(Long id);
}
