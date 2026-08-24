package com.vetsoftware.app.dunning.infrastructure.persistence;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.dunning.application.port.out.DunningSubscriptionPort;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionSnapshot;
import com.vetsoftware.app.dunning.domain.DunningSubscriptionStatus;
import com.vetsoftware.app.dunning.domain.SubscriptionRef;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El puente por el que la cobranza mueve el estado del contrato.
 *
 * <p>
 * <b>Aqui vive la unica escalada de privilegio de la capa de dinero, y esta
 * declarada.</b> {@code ChangeSubscriptionStatusUseCase} esta cerrado a
 * {@code hasRole('SYSTEM')} a secas: mover un contrato entre {@code ACTIVE},
 * {@code PAST_DUE} y {@code READ_ONLY} es una decision de plataforma. Pero la
 * reevaluacion que lo dispara no siempre nace de un principal de plataforma:
 * nace tambien de un pago, y un pago lo puede desencadenar cualquiera que este
 * autorizado a registrarlo.
 */
@Component
public class JpaDunningSubscriptionPort implements DunningSubscriptionPort {

    private final SubscriptionJpaRepository repository;
    private final ChangeSubscriptionStatusUseCase changeStatusUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public JpaDunningSubscriptionPort(SubscriptionJpaRepository repository,
            ChangeSubscriptionStatusUseCase changeStatusUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.repository = repository;
        this.changeStatusUseCase = changeStatusUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public Optional<DunningSubscriptionSnapshot> lockByIdAndCompanyId(Long subscriptionId,
            Long companyId) {
        return repository.lockByIdAndCompanyId(subscriptionId, companyId).map(this::toSnapshot);
    }

    /**
     * Cambia el estado del contrato <b>con la autoridad del sistema, la desencadene
     * quien la desencadene</b>.
     *
     * <p>
     * <b>Por que el {@link SystemAuthRunner} y no dejar que herede el principal del
     * llamador.</b> La cadena real es: alguien aplica un pago que salda una factura
     * vencida, {@code recalculateSettledAmount} baja el saldo a cero, la
     * reevaluacion de mora decide reactivar el contrato y llega aqui. Si esta
     * llamada heredase el principal del que aplico el pago, un actor sin
     * {@code ROLE_SYSTEM} recibiria un {@code AccessDeniedException} <b>dentro de
     * la transaccion</b>, y como todo el flujo va en una sola
     * {@code @Transactional} se revertiria la aplicacion del pago entera: la
     * clinica pago, el sistema le responde 403, no queda rastro del pago y sigue
     * bloqueada en solo lectura. Justo en el momento en que un moroso paga, que es
     * el peor momento posible para descubrirlo.
     *
     * <p>
     * Hoy los seis puertos de mutacion de la capa de dinero estan cerrados a
     * {@code hasRole('SYSTEM')}, asi que ese 403 no se puede provocar. Pero eso es
     * una <b>circunstancia del arbol</b>, no una propiedad del codigo: el dia que
     * cualquiera de esos caminos se abra a un tenant —o que el barrido se dispare
     * desde otro sitio— el fallo aparece sin que nada lo anuncie. La escalada queda
     * declarada aqui, en el unico punto donde ocurre, en vez de depender de quien
     * llame.
     *
     * <p>
     * El runner solo cambia el {@code SecurityContext} mientras dura la llamada y
     * lo restaura despues; no abre transaccion nueva ni ensancha nada mas.
     */
    @Override
    public void changeStatus(Long subscriptionId, Long companyId, DunningSubscriptionStatus status,
            String reason, String actor) {
        systemAuthRunner.run(() -> changeStatusUseCase.execute(new ChangeSubscriptionStatusCommand(
                subscriptionId, companyId,
                com.vetsoftware.app.subscription.domain.SubscriptionStatus.valueOf(status.name()),
                reason, actor)));
    }

    private DunningSubscriptionSnapshot toSnapshot(SubscriptionJpaEntity entity) {
        Long companyId = entity.getCompany().getId();
        SubscriptionRef subscription = new SubscriptionRef(entity.getId(), companyId,
                entity.getSubscriptionNumber(), entity.getStatus().name());
        return new DunningSubscriptionSnapshot(subscription,
                DunningSubscriptionStatus.valueOf(entity.getStatus().name()),
                entity.getGraceDays());
    }
}
