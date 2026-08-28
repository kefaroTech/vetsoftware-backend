package com.vetsoftware.app.entitlement.infrastructure.orchestration;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import com.vetsoftware.app.entitlement.application.port.out.LimitDenialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Escribe el portazo en la bitacora de limites de la empresa.
 *
 * <p>
 * El caso de uso al que delega ya se declara {@code Propagation.REQUIRES_NEW},
 * que es lo que hace que el hecho <strong>sobreviva a la vuelta atras</strong>
 * de la operacion que lo provoco. Sin esa propagacion el hecho se escribiria y
 * se borraria en el mismo suspiro, y la bitacora quedaria vacia justo en los
 * casos que existe para registrar.
 *
 * <p>
 * <strong>Escribir el hecho no puede impedir la negacion.</strong> Si la
 * bitacora falla, lo que tiene que llegarle al usuario sigue siendo "se te
 * acabo el cupo", no un error de servidor: la negacion es correcta y ya estaba
 * decidida cuando llegamos aqui. Por eso se traga la excepcion y se registra
 * como error, en vez de propagarla y convertir un 409 legitimo en un 500.
 */
@Component
public class LimitDenialAdapter implements LimitDenialPort {

    private static final Logger log = LoggerFactory.getLogger(LimitDenialAdapter.class);

    private final RecordLimitEventUseCase recordLimitEvent;

    public LimitDenialAdapter(RecordLimitEventUseCase recordLimitEvent) {
        this.recordLimitEvent = recordLimitEvent;
    }

    @Override
    public void limitDenied(Long companyId, Long limitDimensionId, int limitQuantity,
            int usedQuantity, int requestedDelta) {
        try {
            recordLimitEvent.execute(new RecordLimitEventCommand(companyId, limitDimensionId,
                    LimitEventType.LIMIT_BLOCKED, limitQuantity, usedQuantity, requestedDelta,
                    // De donde salia el techo lo resuelve la bitacora en su propio
                    // recuento: aqui solo consta que el contador vigente dijo que no.
                    LimitSource.SUBSCRIPTION, null, EventActor.automatedProcess(), null, null));
        } catch (RuntimeException noSePudoRegistrar) {
            log.error("No se pudo registrar el portazo de la empresa {} sobre el eje {}"
                    + " (techo {}, usado {}, pedido {}). La negacion SI se aplica: lo que se"
                    + " pierde es la prueba ante una reclamacion y la senal de venta", companyId,
                    limitDimensionId, limitQuantity, usedQuantity, requestedDelta,
                    noSePudoRegistrar);
        }
    }
}
