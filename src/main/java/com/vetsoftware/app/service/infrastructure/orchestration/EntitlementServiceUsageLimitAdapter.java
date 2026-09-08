package com.vetsoftware.app.service.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.companylimitoverride.application.dto.EffectiveLimitDto;
import com.vetsoftware.app.companylimitoverride.application.port.in.ResolveEffectiveLimitUseCase;
import com.vetsoftware.app.service.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.application.port.out.ServiceUsageLimitPort;
import com.vetsoftware.app.service.domain.LimitDimensionRef;
import com.vetsoftware.app.service.domain.ServiceLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SERVICE_ITEM es {@code STOCK}: el contador es
 * {@code COUNT(*) FROM services WHERE company_id = ? AND enabled = TRUE} en
 * vivo, no una bitácora de hechos (por eso no pasa por
 * {@code companyusageevent}). Libera cupo al instante en cuanto se desactiva
 * una tarifa, sin ventana de enfriamiento.
 *
 * <p>
 * <strong>Sin protección atómica contra la carrera</strong>, igual que
 * {@code CheckUsageLimitService}: dos altas concurrentes pueden las dos ver
 * margen y las dos crear. Aceptado a propósito: solo se bloquea crear por
 * encima del techo, no se garantiza precisión bajo concurrencia extrema.
 */
@Component
public class EntitlementServiceUsageLimitAdapter implements ServiceUsageLimitPort {

    private static final Logger log = LoggerFactory
            .getLogger(EntitlementServiceUsageLimitAdapter.class);
    private static final String DIMENSION_CODE = "SERVICE_ITEM";

    private final LimitDimensionQueryPort limitDimensionQueryPort;
    private final ServiceRepository serviceRepository;
    private final ResolveEffectiveLimitUseCase resolveEffectiveLimit;
    private final RecordLimitEventUseCase recordLimitEvent;
    private final SystemAuthRunner systemAuthRunner;
    private final Authz authz;

    public EntitlementServiceUsageLimitAdapter(LimitDimensionQueryPort limitDimensionQueryPort,
            ServiceRepository serviceRepository, ResolveEffectiveLimitUseCase resolveEffectiveLimit,
            RecordLimitEventUseCase recordLimitEvent, SystemAuthRunner systemAuthRunner,
            Authz authz) {
        this.limitDimensionQueryPort = limitDimensionQueryPort;
        this.serviceRepository = serviceRepository;
        this.resolveEffectiveLimit = resolveEffectiveLimit;
        this.recordLimitEvent = recordLimitEvent;
        this.systemAuthRunner = systemAuthRunner;
        this.authz = authz;
    }

    @Override
    public void checkNotExceeded(Long companyId) {
        LimitDimensionRef dimension = limitDimensionQueryPort.findByCode(DIMENSION_CODE)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown limit dimension code: " + DIMENSION_CODE));
        // Resolver el techo vigente exige la autoridad companyLimitOverride.read, que
        // ningun rol operativo siembra: corre como SYSTEM por el mismo motivo que
        // ResolveEffectiveUsageLimitAdapter.
        EffectiveLimitDto limit = systemAuthRunner
                .call(() -> resolveEffectiveLimit.resolve(companyId, dimension.id()));
        if (limit.unlimited()) {
            return;
        }
        long used = serviceRepository.countEnabledByCompanyId(companyId);
        if (used + 1 > limit.limitQuantity()) {
            denyAndRecord(companyId, dimension.id(), limit.limitQuantity(), (int) used);
            throw new ServiceLimitExceededException(companyId, DIMENSION_CODE,
                    limit.limitQuantity(), (int) used, 1);
        }
    }

    private void denyAndRecord(Long companyId, Long limitDimensionId, int limitQuantity,
            int usedQuantity) {
        try {
            recordLimitEvent.execute(new RecordLimitEventCommand(companyId, limitDimensionId,
                    LimitEventType.LIMIT_BLOCKED, limitQuantity, usedQuantity, 1,
                    LimitSource.SUBSCRIPTION, null, actor(), null, null));
        } catch (RuntimeException noSePudoRegistrar) {
            log.error(
                    "No se pudo registrar el portazo de la empresa {} sobre el eje {}"
                            + " (techo {}, usado {}, pedido 1). La negacion SI se aplica: lo que se"
                            + " pierde es la prueba ante una reclamacion y la senal de venta",
                    companyId, limitDimensionId, limitQuantity, usedQuantity, noSePudoRegistrar);
        }
    }

    private EventActor actor() {
        Long employeeId = authz.currentEmployeeIdOrNull();
        return employeeId == null ? EventActor.automatedProcess() : EventActor.employee(employeeId);
    }
}
