package com.vetsoftware.app.pricelist.application.port.in;

import com.vetsoftware.app.pricelist.application.command.CreatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Tarifa global de plataforma: no hay {@code companyId} que validar, asi que el
 * gate es {@code hasRole('SYSTEM')} a secas.
 */
public interface CreatePriceListUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PriceListDto execute(CreatePriceListCommand command);
}
