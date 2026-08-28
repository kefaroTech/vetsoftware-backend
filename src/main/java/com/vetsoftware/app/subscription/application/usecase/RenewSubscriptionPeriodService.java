package com.vetsoftware.app.subscription.application.usecase;

import com.vetsoftware.app.subscription.application.command.RenewSubscriptionPeriodCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.in.RenewSubscriptionPeriodUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mueve el periodo facturado del contrato al siguiente.
 *
 * <p>
 * <b>Es el llamador de produccion que a {@code Subscription.renewPeriod} le
 * faltaba.</b> Pasa por el dominio y no por un {@code UPDATE} directo porque el
 * dominio es quien comprueba que el periodo no nazca invertido; un
 * {@code UPDATE} con las fechas cambiadas de orden habria entrado sin una queja
 * y dejado un contrato cuyo periodo termina antes de empezar.
 *
 * <p>
 * <b>Carga con bloqueo pesimista y acotada por empresa.</b> El bloqueo
 * serializa el <em>leer-y-luego-escribir</em> frente al barrido de lifecycle,
 * que puede estar cambiando el estado del mismo contrato; la cota por empresa
 * es {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} —el puerto no declara ninguna
 * variante ancha, asi que no hay nada que llamar por error—.
 */
@Service
public class RenewSubscriptionPeriodService implements RenewSubscriptionPeriodUseCase {

    private final SubscriptionRepository repository;

    public RenewSubscriptionPeriodService(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SubscriptionDto execute(RenewSubscriptionPeriodCommand command) {
        if (command.subscriptionId() == null)
            throw new IllegalArgumentException("subscriptionId is required");
        if (command.companyId() == null)
            throw new IllegalArgumentException("companyId is required");
        Subscription subscription = repository
                .lockByIdAndCompanyId(command.subscriptionId(), command.companyId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));
        // El dominio valida el orden de las fechas; aqui no se repite la comprobacion
        // para que exista una sola version de esa regla.
        subscription.renewPeriod(command.periodStart(), command.periodEnd(),
                command.nextBillingDate());
        return SubscriptionDto.from(repository.save(subscription));
    }
}
