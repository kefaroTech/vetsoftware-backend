package com.vetsoftware.app.platformbillingconfig.infrastructure.web;

import com.vetsoftware.app.platformbillingconfig.application.command.UpdatePlatformBillingConfigCommand;
import com.vetsoftware.app.platformbillingconfig.application.dto.PlatformBillingConfigDto;
import com.vetsoftware.app.platformbillingconfig.application.dto.PriceListSummaryDto;
import com.vetsoftware.app.platformbillingconfig.application.port.in.FindPlatformBillingConfigUseCase;
import com.vetsoftware.app.platformbillingconfig.application.port.in.UpdatePlatformBillingConfigUseCase;
import com.vetsoftware.app.platformbillingconfig.infrastructure.web.request.UpdatePlatformBillingConfigRequest;
import com.vetsoftware.app.platformbillingconfig.infrastructure.web.response.PlatformBillingConfigResponse;
import com.vetsoftware.app.platformbillingconfig.infrastructure.web.response.PriceListSummary;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración de facturación de la plataforma: días de gracia, días de
 * prueba, día de emisión, plazo de pago, tarifa por defecto y proveedor de
 * facturación externa.
 *
 * <p>
 * <b>Recurso singular, no colección.</b> La ruta no tiene {@code /{id}} ni
 * listado porque la tabla tiene exactamente una fila garantizada por el
 * esquema. Dos verbos y nada más: {@code GET} para leerla y {@code PUT} para
 * reemplazarla. No hay {@code POST}: la fila la siembra el changeset que crea
 * la tabla, no un alta desde la interfaz.
 *
 * <p>
 * No usa {@code Authz}: no hay empresa que inyectar. Los dos casos de uso están
 * cerrados a {@code hasRole('SYSTEM')} a secas en su {@code port/in}.
 */
@RestController
@RequestMapping("/platform-billing-config")
public class PlatformBillingConfigController {
    private final FindPlatformBillingConfigUseCase findUseCase;
    private final UpdatePlatformBillingConfigUseCase updateUseCase;

    public PlatformBillingConfigController(FindPlatformBillingConfigUseCase findUseCase,
            UpdatePlatformBillingConfigUseCase updateUseCase) {
        this.findUseCase = findUseCase;
        this.updateUseCase = updateUseCase;
    }

    @GetMapping
    public PlatformBillingConfigResponse find() {
        return toResponse(findUseCase.find());
    }

    @PutMapping
    public PlatformBillingConfigResponse update(
            @Valid @RequestBody UpdatePlatformBillingConfigRequest request) {
        return toResponse(updateUseCase.execute(new UpdatePlatformBillingConfigCommand(
                request.defaultPriceListId(), request.defaultGraceDays(),
                request.defaultTrialDays(), request.invoiceDayOfMonth(),
                request.defaultPaymentTermDays(), request.externalBillingProvider())));
    }

    private PlatformBillingConfigResponse toResponse(PlatformBillingConfigDto dto) {
        PriceListSummaryDto p = dto.defaultPriceList();
        return new PlatformBillingConfigResponse(dto.id(),
                p == null ? null : new PriceListSummary(p.id(), p.code(), p.name()),
                dto.defaultGraceDays(), dto.defaultTrialDays(), dto.invoiceDayOfMonth(),
                dto.defaultPaymentTermDays(), dto.externalBillingProvider(), dto.createdDate());
    }
}
