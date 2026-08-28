package com.vetsoftware.app.companylimitevent.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitevent.application.command.AdjustCompanyUsageCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.AdjustCompanyUsageUseCase;
import com.vetsoftware.app.companylimitevent.application.port.in.ListCompanyLimitEventsUseCase;
import com.vetsoftware.app.companylimitevent.application.port.in.ReconcileCompanyUsageUseCase;
import com.vetsoftware.app.companylimitevent.infrastructure.web.request.AdjustCompanyUsageRequest;
import com.vetsoftware.app.companylimitevent.infrastructure.web.response.CompanyLimitEventResponse;
import com.vetsoftware.app.companylimitevent.infrastructure.web.response.UsageReconciliationResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La consola de plataforma sobre la bitácora de cupo: auditar, corregir y
 * recontar.
 *
 * <p>
 * <strong>La empresa entra por la ruta.</strong> No viaja en ningún cuerpo —lo
 * prohíbe {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}— ni se deriva del principal,
 * porque un usuario de plataforma no tiene empresa. Y <strong>quién firma lo
 * pone el servidor</strong>, con {@code authz.currentSystemUserId()}: un
 * firmante que escribe el llamador no es una firma.
 *
 * <p>
 * <strong>La corrección de consumo está cerrada a {@code hasRole('SYSTEM')} y
 * eso es media razón de que exista</strong> (D-12, R-LIMIT-19). La propia ficha
 * de construcción avisa del defecto que se estaba a punto de cometer: si la
 * corrección aterrizara en un puerto cuya autorización ya admite al cliente, la
 * administradora de la clínica recuperaría su cupo cada vez que topa y el cupo
 * dejaría de existir sin que ninguna fila del modelo estuviera mal. Por eso es
 * un caso de uso distinto del que mueve el contador durante una operación
 * normal, y por eso vive en este controlador y no en el del tenant.
 *
 * <p>
 * <strong>El recuento cuelga de una ruta plana</strong> porque recorre los
 * contadores de todas las empresas: no hay empresa que acotar
 * ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}) y, aunque la hubiera, quien puede
 * declarar comprobado su propio contador puede declararlo sano sin contar nada.
 * No sobrescribe el contador: escribe un hecho compensatorio y sella solo lo
 * que cuadra.
 */
@RestController
@RequestMapping("/system/company-limit-events")
public class SystemCompanyLimitEventController {

    private final ListCompanyLimitEventsUseCase listUseCase;
    private final AdjustCompanyUsageUseCase adjustUseCase;
    private final ReconcileCompanyUsageUseCase reconcileUseCase;
    private final Authz authz;

    public SystemCompanyLimitEventController(ListCompanyLimitEventsUseCase listUseCase,
            AdjustCompanyUsageUseCase adjustUseCase, ReconcileCompanyUsageUseCase reconcileUseCase,
            Authz authz) {
        this.listUseCase = listUseCase;
        this.adjustUseCase = adjustUseCase;
        this.reconcileUseCase = reconcileUseCase;
        this.authz = authz;
    }

    /**
     * La bitácora de una clínica, vista desde plataforma. Es el mismo puerto que
     * sirve a {@link CompanyLimitEventController}: su {@code @PreAuthorize} admite
     * {@code hasRole('SYSTEM')} <em>o</em> la propia empresa, y aquí entra por la
     * primera mitad.
     */
    @GetMapping("/companies/{companyId}")
    public List<CompanyLimitEventResponse> listByCompany(@PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return listUseCase.listByCompanyId(companyId, from, to).stream()
                .map(CompanyLimitEventResponse::from).toList();
    }

    /**
     * La válvula de escape de D-12: corrige el consumo y deja escrito el hecho que
     * lo compensa, con motivo obligatorio y firma. No sobrescribe el contador — lo
     * mueve con la instrucción atómica de siempre.
     */
    @PostMapping("/companies/{companyId}/usage-adjustments")
    public CompanyLimitEventResponse adjustUsage(@PathVariable Long companyId,
            @Valid @RequestBody AdjustCompanyUsageRequest request) {
        return CompanyLimitEventResponse
                .from(adjustUseCase.execute(new AdjustCompanyUsageCommand(companyId,
                        request.limitDimensionId(), request.capacityUnit(), request.delta(),
                        authz.currentSystemUserId(), request.reasonCode(), request.reason())));
    }

    /**
     * Una pasada del recuento de R-LIMIT-30. Devuelve el cursor del lote para poder
     * pedir el siguiente: cero contadores examinados es una respuesta correcta.
     */
    @PostMapping("/reconciliations")
    public UsageReconciliationResponse reconcile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime staleBefore,
            @RequestParam(defaultValue = "0") long afterId,
            @RequestParam(defaultValue = "200") int batchSize) {
        return UsageReconciliationResponse
                .from(reconcileUseCase.execute(staleBefore, afterId, batchSize), batchSize);
    }
}
