package com.vetsoftware.app.subscriptionmodule.application.port.in;

import com.vetsoftware.app.subscriptionmodule.application.command.ListModuleShowcaseQuery;
import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleShowcaseDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * «Tus módulos»: qué tiene la empresa, en qué estado, y qué podría comprar.
 * Lectura disponible para cualquier empleado de la empresa —el botón de compra
 * lo decide {@code canPurchase}, no este gate—.
 */
public interface ListModuleShowcaseUseCase {

    @PreAuthorize("hasRole('SYSTEM') or @authz.isMyCompany(#query.companyId)")
    List<ModuleShowcaseDto> execute(ListModuleShowcaseQuery query);
}
