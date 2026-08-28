package com.vetsoftware.app.withholdingraterule.infrastructure.web;

import com.vetsoftware.app.withholdingraterule.application.command.CloseWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.command.CreateWithholdingRateRuleCommand;
import com.vetsoftware.app.withholdingraterule.application.port.in.CloseWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.application.port.in.CreateWithholdingRateRuleUseCase;
import com.vetsoftware.app.withholdingraterule.infrastructure.web.request.CloseWithholdingRateRuleRequest;
import com.vetsoftware.app.withholdingraterule.infrastructure.web.request.CreateWithholdingRateRuleRequest;
import com.vetsoftware.app.withholdingraterule.infrastructure.web.response.WithholdingRateRuleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * El unico sitio desde el que se escribe el catalogo de retenciones: alta de
 * una tarifa y cierre de su vigencia.
 *
 * <p>
 * <strong>Aqui no hay {@code companyId} por ninguna via, y esa ausencia se
 * explica distinto que en los demas controllers de plataforma.</strong> En
 * tesoreria la empresa viaja como {@code @RequestParam} porque un principal
 * SYSTEM no tiene empresa propia y elige a que clinica le afecta. En este
 * bloque no hay a quien elegir: {@code withholding_rate_rules} es un catalogo
 * global, la tarifa depende del supuesto fiscal y no del cliente, y lo que si
 * depende del cliente —si es agente de retencion— vive en
 * {@code company_billing_profiles}.
 *
 * <p>
 * <strong>No hay endpoint de borrado, y tampoco de deshabilitado.</strong> Una
 * tarifa que dejo de aplicarse sigue siendo la correcta para las facturas de su
 * vigencia; lo que se hace es <em>cerrarla</em>, que ademas es lo que libera el
 * hueco de {@code uq_withholding_rate_rules_current} para publicar su relevo.
 * Borrarla dejaria sin explicacion las retenciones ya calculadas.
 */
@RestController
@RequestMapping("/system/withholding-rate-rules")
public class SystemWithholdingRateRuleController {

    private final CreateWithholdingRateRuleUseCase createUseCase;
    private final CloseWithholdingRateRuleUseCase closeUseCase;

    public SystemWithholdingRateRuleController(CreateWithholdingRateRuleUseCase createUseCase,
            CloseWithholdingRateRuleUseCase closeUseCase) {
        this.createUseCase = createUseCase;
        this.closeUseCase = closeUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WithholdingRateRuleResponse create(
            @Valid @RequestBody CreateWithholdingRateRuleRequest request) {
        return WithholdingRateRuleResponse.from(createUseCase
                .execute(new CreateWithholdingRateRuleCommand(request.withholdingType(),
                        request.serviceNature(), request.municipalityCode(), request.ratePercent(),
                        request.minimumBaseAmount(), request.minimumBaseUvt(),
                        request.legalReference(), request.validFrom(), request.validTo())));
    }

    /**
     * {@code PATCH} y no {@code DELETE}: cerrar una vigencia es escribir una fecha
     * en una fila que se queda, no retirarla.
     */
    @PatchMapping("/{id}/close")
    public WithholdingRateRuleResponse close(@PathVariable Long id,
            @Valid @RequestBody CloseWithholdingRateRuleRequest request) {
        return WithholdingRateRuleResponse.from(
                closeUseCase.execute(new CloseWithholdingRateRuleCommand(id, request.validTo())));
    }
}
