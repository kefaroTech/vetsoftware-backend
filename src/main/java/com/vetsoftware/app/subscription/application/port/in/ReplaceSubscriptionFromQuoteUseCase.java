package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.ReplaceSubscriptionFromQuoteCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>El eslabon que faltaba entre el embudo comercial y el contrato
 * (DC-2).</strong> Cierra el contrato vigente de la empresa y abre el que
 * describe la cotizacion aceptada, en una sola transaccion.
 *
 * <p>
 * <strong>Por que sustituir y no ampliar.</strong> Decision del dueño del
 * producto: cada cotizacion aceptada produce <em>su</em> contrato, con su
 * numero y su expediente, en vez de un otrosi sobre el que ya habia. La
 * alternativa —un otrosi— conservaba el numero y la bitacora y estaba
 * disponible ({@code AddSubscriptionItemUseCase} ya acepta {@code quoteId}); se
 * descarto a proposito.
 *
 * <p>
 * <strong>Por que las dos mitades no se pueden separar.</strong>
 * {@code uq_subscriptions_active_company}, sobre la columna generada
 * {@code active_marker}, no admite dos contratos vigentes de la misma empresa.
 * Luego cerrar y abrir <em>tienen</em> que ocurrir en la misma transaccion: si
 * se partieran en dos, un fallo entre medias dejaria a la empresa con
 * <strong>ninguno</strong> —sin {@code company_entitlements}, dentro del
 * sistema y sin poder hacer nada, que es el estado que
 * {@code PlatformCatalogSubscriptionCreator} lleva documentado como
 * inaceptable—. El orden dentro de la transaccion tambien importa, y funciona
 * porque {@code JpaSubscriptionRepository.save} hace {@code saveAndFlush}: el
 * cierre llega a la base antes de que se intente la apertura, en vez de
 * quedarse en la cola de acciones de Hibernate, que ejecuta los {@code INSERT}
 * antes que los {@code UPDATE} y haria saltar el unique con las dos operaciones
 * correctas.
 *
 * <p>
 * <strong>Es de la plataforma, por escrito.</strong> Firmar un contrato no es
 * una operacion del cliente: el gate es {@code hasRole('SYSTEM')} a secas, y
 * quien lo alcanza es el adaptador de orquestacion de {@code quote}, que escala
 * con {@code SystemAuthRunner} despues de que {@code AcceptQuoteUseCase} haya
 * revalidado la empresa del principal. Es el mismo cableado con el que
 * {@code registration} acuña el primer contrato de una empresa y con el que
 * {@code SelfServeQuoteUseCase} emite su propia oferta.
 *
 * <p>
 * <strong>El contrato nace sin cobrar.</strong> Todavia no hay pasarela: nace
 * en {@code TRIALING} —o en {@code PAST_DUE} si la plataforma no concede
 * prueba, ver el servicio— y lo activa {@link SettleNewContractUseCase} cuando
 * el pago se aprueba. Si el pago no llega nunca, se queda donde nacio, que es
 * un estado que el modelo ya maneja y que <em>no</em> le quita el acceso al
 * cliente (R18).
 */
public interface ReplaceSubscriptionFromQuoteUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionDto execute(ReplaceSubscriptionFromQuoteCommand command);
}
