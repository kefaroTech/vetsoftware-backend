package com.vetsoftware.app.companyusageevent.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.companyusageevent.application.port.out.UsageLimitDenialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Escribe el portazo en la bitácora de límites de la empresa, mismo patrón que
 * {@code entitlement.infrastructure.orchestration.LimitDenialAdapter}.
 *
 * <p>
 * <strong>Corre con el principal del empleado que topó con el techo</strong> (a
 * diferencia del adaptador que resuelve el techo vigente): el gate de
 * {@code RecordLimitEventUseCase} es la empresa, no una autoridad, así que no
 * hace falta {@code SystemAuthRunner} aquí.
 *
 * <p>
 * El origen del techo se declara siempre {@link LimitSource#SUBSCRIPTION}: de
 * dónde salía exactamente lo resuelve la propia bitácora en su recuento; aquí
 * solo consta que el contador vigente dijo que no. Igual que
 * {@code LimitDenialAdapter}, se traga la excepción: la negación ya está
 * decidida y correcta, y no puede convertirse en un 500 porque la bitácora
 * falle.
 */
@Component
public class RecordUsageLimitDenialAdapter implements UsageLimitDenialPort {

    private static final Logger log = LoggerFactory.getLogger(RecordUsageLimitDenialAdapter.class);

    private final RecordLimitEventUseCase recordLimitEvent;
    private final Authz authz;

    public RecordUsageLimitDenialAdapter(RecordLimitEventUseCase recordLimitEvent, Authz authz) {
        this.recordLimitEvent = recordLimitEvent;
        this.authz = authz;
    }

    @Override
    public void limitDenied(Long companyId, Long limitDimensionId, int limitQuantity,
            int usedQuantity, int requestedDelta) {
        try {
            recordLimitEvent.execute(new RecordLimitEventCommand(companyId, limitDimensionId,
                    LimitEventType.LIMIT_BLOCKED, limitQuantity, usedQuantity, requestedDelta,
                    LimitSource.SUBSCRIPTION, null, actor(), null, null));
        } catch (RuntimeException noSePudoRegistrar) {
            log.error("No se pudo registrar el portazo de la empresa {} sobre el eje {}"
                    + " (techo {}, usado {}, pedido {}). La negacion SI se aplica: lo que se"
                    + " pierde es la prueba ante una reclamacion y la senal de venta", companyId,
                    limitDimensionId, limitQuantity, usedQuantity, requestedDelta,
                    noSePudoRegistrar);
        }
    }

    private EventActor actor() {
        Long employeeId = authz.currentEmployeeIdOrNull();
        return employeeId == null ? EventActor.automatedProcess() : EventActor.employee(employeeId);
    }
}
