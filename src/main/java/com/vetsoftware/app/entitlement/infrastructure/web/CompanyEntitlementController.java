package com.vetsoftware.app.entitlement.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyAccessDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.entitlement.application.port.in.FindCompanyAccessUseCase;
import com.vetsoftware.app.entitlement.application.port.in.ListCompanyEntitlementsUseCase;
import com.vetsoftware.app.entitlement.application.port.in.RecalculateCompanyEntitlementsUseCase;
import com.vetsoftware.app.entitlement.infrastructure.web.response.CompanyAccessResponse;
import com.vetsoftware.app.entitlement.infrastructure.web.response.CompanyCapacityResponse;
import com.vetsoftware.app.entitlement.infrastructure.web.response.CompanyEntitlementResponse;
import com.vetsoftware.app.entitlement.infrastructure.web.response.EntitlementRecalculationResponse;
import com.vetsoftware.app.entitlement.infrastructure.web.response.SubModuleSummary;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import org.springframework.web.bind.annotation.*;

/**
 * Los permisos derivados de la empresa del usuario.
 *
 * <p>
 * <strong>Ningun endpoint recibe {@code companyId}</strong>, ni en la ruta ni
 * en el cuerpo: la pone el controller con {@code authz.currentCompanyId()}
 * desde el principal, y el puerto la revalida con
 * {@code @authz.isMyCompany(...)}. Dejar que el cliente eligiera la empresa
 * aqui seria regalar los permisos de cualquier clinica a quien supiera escribir
 * un numero.
 *
 * <p>
 * Los tres endpoints son de lectura o de reparacion y ninguno lleva cuerpo, asi
 * que no hay {@code @RequestBody} que validar.
 */
@RestController
@RequestMapping("/entitlements")
public class CompanyEntitlementController {

    private final FindCompanyAccessUseCase findAccessUseCase;
    private final ListCompanyEntitlementsUseCase listUseCase;
    private final RecalculateCompanyEntitlementsUseCase recalculateUseCase;
    private final Authz authz;

    public CompanyEntitlementController(FindCompanyAccessUseCase findAccessUseCase,
            ListCompanyEntitlementsUseCase listUseCase,
            RecalculateCompanyEntitlementsUseCase recalculateUseCase, Authz authz) {
        this.findAccessUseCase = findAccessUseCase;
        this.listUseCase = listUseCase;
        this.recalculateUseCase = recalculateUseCase;
        this.authz = authz;
    }

    /** La consulta caliente: que puede usar mi clinica ahora mismo. */
    @GetMapping("/access")
    public CompanyAccessResponse currentAccess() {
        return toAccessResponse(findAccessUseCase.findByCompanyId(authz.currentCompanyId()));
    }

    /**
     * El listado de auditoria, con los caducados y los ocultos. Paginado con el
     * contrato unico del proyecto.
     */
    @GetMapping
    public PageResponse<CompanyEntitlementResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listByCompanyId(authz.currentCompanyId(), page, pageSize),
                CompanyEntitlementController::toEntitlementResponse);
    }

    /**
     * Fuerza el recalculo. No es el camino normal --el normal es que lo dispare
     * cada cambio de contrato-- sino la palanca para cuando {@code recalculatedAt}
     * se ha quedado viejo y hay que reparar una empresa sin esperar a su siguiente
     * movimiento.
     */
    @PostMapping("/recalculate")
    public EntitlementRecalculationResponse recalculate() {
        return toRecalculationResponse(recalculateUseCase
                .execute(new RecalculateCompanyEntitlementsCommand(authz.currentCompanyId())));
    }

    private static CompanyAccessResponse toAccessResponse(CompanyAccessDto dto) {
        return new CompanyAccessResponse(dto.companyId(),
                dto.entitlements().stream().map(CompanyEntitlementController::toEntitlementResponse)
                        .toList(),
                dto.capacities().stream().map(CompanyEntitlementController::toCapacityResponse)
                        .toList(),
                dto.recalculatedAt());
    }

    private static CompanyEntitlementResponse toEntitlementResponse(CompanyEntitlementDto dto) {
        SubModuleSummaryDto subModule = dto.subModule();
        return new CompanyEntitlementResponse(dto.id(), dto.companyId(),
                new SubModuleSummary(subModule.id(), subModule.code(), subModule.name()),
                dto.accessLevel(), dto.source(), dto.subscriptionId(), dto.subscriptionItemId(),
                dto.validFrom(), dto.validUntil(), dto.recalculatedAt());
    }

    private static CompanyCapacityResponse toCapacityResponse(CompanyCapacityDto dto) {
        return new CompanyCapacityResponse(dto.id(), dto.companyId(), dto.capacityUnit(),
                dto.limitQuantity(), dto.usedQuantity(), dto.exhausted(), dto.subscriptionId(),
                dto.recalculatedAt());
    }

    private static EntitlementRecalculationResponse toRecalculationResponse(
            EntitlementRecalculationDto dto) {
        return new EntitlementRecalculationResponse(dto.companyId(), dto.subscriptionId(),
                dto.contractStatus(), dto.entitlementCount(), dto.manualGrantCount(),
                dto.capacityCount(), dto.recalculatedAt());
    }
}
