package com.vetsoftware.app.electronicdocument.infrastructure.orchestration;

import com.vetsoftware.app.electronicdocument.application.port.out.AccountReversalPort;
import com.vetsoftware.app.openaccount.application.command.MarkOpenAccountReversedCommand;
import com.vetsoftware.app.openaccount.application.port.in.MarkOpenAccountReversedUseCase;
import org.springframework.stereotype.Component;

/**
 * Único punto que conecta la validación DIAN de una nota crédito con el reverso
 * de cartera. Es el gemelo de {@link ClosedAccountEmissionAdapter} en el otro
 * sentido: la dependencia sigue yendo electronicdocument → openaccount y la
 * regla del reverso —guarda de estado e idempotencia— se queda entera en el
 * dominio de openaccount, que es quien la define.
 *
 * <p>
 * <b>Vive en {@code orchestration} y no en {@code persistence} a propósito.</b>
 * Su antecesor, {@code JpaAccountReversalPort}, hablaba con el repositorio JPA
 * de la otra feature y estampaba la fila a mano; este no toca persistencia
 * ninguna, así que quedarse en aquel paquete sería mentir sobre lo que hace.
 *
 * <p>
 * <b>Cambio de comportamiento deliberado (incidencia #124)</b>: reversar una
 * cuenta que no esté CLOSE ahora <b>lanza</b> dentro de la transacción del
 * webhook, donde antes se estampaba en silencio. Hoy es inalcanzable —
 * {@code DocumentBuilder} exige CLOSE antes de construir la factura—, así que
 * en la práctica funciona como tripwire: si alguna vez salta, es que apareció
 * un segundo llamador. No lo tapes con un {@code try/catch}.
 */
@Component
public class AccountReversalAdapter implements AccountReversalPort {
    private final MarkOpenAccountReversedUseCase markReversed;

    public AccountReversalAdapter(MarkOpenAccountReversedUseCase markReversed) {
        this.markReversed = markReversed;
    }

    @Override
    public void markReversed(Long openAccountId, Long companyId) {
        // reversedAt null: la fecha la pone el dominio.
        markReversed.execute(new MarkOpenAccountReversedCommand(openAccountId, companyId, null));
    }
}
