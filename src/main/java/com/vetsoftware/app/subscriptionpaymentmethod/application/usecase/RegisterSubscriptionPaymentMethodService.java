package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.RegisterSubscriptionPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.RegisterSubscriptionPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodTokenAlreadyRegisteredException;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Da de alta el medio con el que se le cobrara a la clinica.
 *
 * <p>
 * <strong>El testigo se comprueba antes de insertar.</strong>
 * {@code uq_subscription_payment_methods_token} convierte el duplicado en un
 * error del motor, y un 500 en la cara del cliente no explica nada; la busqueda
 * previa, dentro de la transaccion, devuelve el medio que ya existe cuando es
 * suyo y un 409 limpio cuando no lo es.
 */
@Observed(name = "subscription.payment.method.register")
@Service
public class RegisterSubscriptionPaymentMethodService
        implements
            RegisterSubscriptionPaymentMethodUseCase {

    private final SubscriptionPaymentMethodRepository repository;
    private final Clock clock;

    public RegisterSubscriptionPaymentMethodService(SubscriptionPaymentMethodRepository repository,
            Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubscriptionPaymentMethodDto execute(RegisterSubscriptionPaymentMethodCommand command) {
        Optional<SubscriptionPaymentMethod> existing = findByToken(command);
        if (existing.isPresent())
            return SubscriptionPaymentMethodDto.from(existing.get());

        SubscriptionPaymentMethod paymentMethod = SubscriptionPaymentMethod.register(
                command.companyId(), command.methodKind(), command.gateway(), command.token(),
                command.brand(), command.lastFour(), command.expiresOn(), command.mandateEvidence(),
                command.authorizedAt(), LocalDateTime.now(clock));
        return SubscriptionPaymentMethodDto.from(repository.save(paymentMethod));
    }

    /**
     * Reenviar el alta con el mismo testigo devuelve el medio ya registrado en vez
     * de reventar contra la unicidad.
     *
     * <p>
     * <strong>Ojo con el aislamiento aqui:</strong> la unicidad del testigo es
     * global, no por empresa, asi que este finder puede devolver el medio de
     * <em>otra</em> clinica. Devolverlo seria enseñarle su tarjeta —marca, ultimos
     * cuatro y fecha— a quien pregunta. Cuando el testigo ya esta tomado por otra
     * empresa se rechaza sin decir de quien es: es un conflicto, no un resultado.
     */
    private Optional<SubscriptionPaymentMethod> findByToken(
            RegisterSubscriptionPaymentMethodCommand command) {
        if (command.gateway() == null || command.token() == null)
            return Optional.empty();
        Optional<SubscriptionPaymentMethod> existing = repository
                .findByGatewayAndToken(command.gateway(), command.token());
        if (existing.isEmpty())
            return Optional.empty();
        if (!existing.get().getCompanyId().equals(command.companyId()))
            throw new PaymentMethodTokenAlreadyRegisteredException(command.gateway());
        return existing;
    }
}
