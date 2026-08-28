package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.SetDefaultPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.in.SetDefaultPaymentMethodUseCase;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethodNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Elige con que se cobra por defecto. <strong>Uno solo por empresa.</strong>
 *
 * <p>
 * <strong>La barandilla la pone la base; esto evita que la barandilla se
 * note.</strong> {@code default_marker} es una columna generada que proyecta la
 * empresa solo cuando {@code is_default} esta puesto <em>y</em> el mandato
 * sigue {@code ACTIVE}, y {@code uq_subscription_payment_methods_default} la
 * hace unica. La exclusividad esta garantizada pase lo que pase aqui; lo que
 * aporta este servicio es que el cliente reciba un resultado en vez de un 500.
 *
 * <p>
 * <strong>El orden es la parte delicada, y por eso la limpieza no es un
 * {@code save}.</strong> Dos {@code save} en la misma transaccion dejan en
 * manos de Hibernate cuando se vacia cada {@code UPDATE}, y la unicidad se
 * comprueba por instruccion y no al cierre: si el motor viera primero el que
 * marca el nuevo, habria dos marcadores iguales y rechazaria la operacion. La
 * limpieza va por una escritura acotada que se ejecuta en el acto, antes de
 * marcar.
 *
 * <p>
 * <strong>Y no hay nada que limpiar cuando el anterior se revoco</strong>: al
 * dejar de estar {@code ACTIVE} su {@code default_marker} ya vale {@code NULL}
 * y el hueco se libero solo. Es justo lo que hace falta el dia que el cliente
 * cambia de tarjeta — y es tambien por lo que la limpieza no toca los mandatos
 * revocados: su {@code is_default} es el rastro de cual lo fue, y borrarlo no
 * libera ningun hueco porque ese hueco ya estaba libre.
 */
@Observed(name = "subscription.payment.method.set.default")
@Service
public class SetDefaultPaymentMethodService implements SetDefaultPaymentMethodUseCase {

    private final SubscriptionPaymentMethodRepository repository;

    public SetDefaultPaymentMethodService(SubscriptionPaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SubscriptionPaymentMethodDto execute(SetDefaultPaymentMethodCommand command) {
        SubscriptionPaymentMethod target = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new SubscriptionPaymentMethodNotFoundException(command.id()));

        // Excluye el propio objetivo: volver a marcar el que ya lo era es
        // idempotente y no debe dejar a la empresa sin predeterminado por el camino.
        repository.clearDefaultForCompany(command.companyId(), command.id());
        target.makeDefault();
        return SubscriptionPaymentMethodDto.from(repository.save(target));
    }
}
