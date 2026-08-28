package com.vetsoftware.app.subscriptionbilling.application.port.out;

import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionItemBillingProfile;
import java.util.Optional;

/**
 * La FK compuesta {@code fk_subscription_charges_item} sobre
 * {@code (company_id, subscription_item_id)}, comprobada antes de construir el
 * cargo -- <b>y el modo de cobro de esa linea</b>, que es lo unico que decide
 * si devenga.
 *
 * <p>
 * <b>Sigue siendo un {@code ValidationPort} y no un {@code QueryPort} pese a
 * devolver el modo.</b> El cargo no copia ni un campo de la linea del contrato:
 * congela su propia descripcion, su cantidad y su tarifa en el momento de
 * devengarse. {@link ItemChargeMode} no es un dato que el cargo guarde, es la
 * respuesta a "esta linea puede devengar", que es exactamente lo que valida
 * este puerto; traer un {@code SubscriptionItemRef} entero seria copiar campos
 * que nadie usa y atar la capa de dinero a la forma de
 * {@code SubscriptionItemJpaEntity}.
 *
 * <p>
 * <b>Solo declara variantes acotadas por empresa, y eso es deliberado.</b> La
 * forma ancha --{@code existsById(itemId)}-- no rechazaria nada util:
 * permitiria colgar el cargo de la clinica A de una linea del contrato de la
 * clinica B. La base lo rechaza igual por la FK compuesta, pero como una
 * violacion de constraint convertida en 500 a mitad del cierre mensual; aqui
 * devuelve vacio y el caso de uso dice cual de los cinco ids del cuerpo estaba
 * mal.
 *
 * <p>
 * Devuelve un {@code Optional} en vez de lanzar: la excepcion de FK inexistente
 * y la de linea que no cobra las decide el caso de uso, nunca el adaptador.
 */
public interface SubscriptionItemValidationPort {

    /**
     * El modo de cobro de la linea, si existe <b>y es de esa empresa</b>.
     *
     * <p>
     * <b>Este es el metodo con el que se decide si algo se cobra</b> (R-TRIAL-14).
     * {@link #existsInCompany} responde a otra pregunta -- que la FK cuadra -- y
     * una linea en {@code TRIAL} la cumple perfectamente: existir no es permiso
     * para cobrar.
     *
     * <p>
     * Comprueba el par {@code (company_id, id)} de la FK compuesta y nada mas. En
     * concreto <b>no</b> mira {@code enabled} ni la vigencia: devengar el cargo de
     * un periodo ya cerrado contra la linea que lo presto es legitimo, y ser mas
     * estricto que la base rechazaria cierres correctos. Tampoco mira el estado del
     * contrato: eso es justo lo que R-TRIAL-13 prohibe.
     *
     * @return vacio si la linea no existe o es de otra empresa
     */
    Optional<ItemChargeMode> findChargeModeInCompany(Long subscriptionItemId, Long companyId);

    /**
     * El modo de cobro de la linea <b>y su impuesto</b>, si existe y es de esa
     * empresa.
     *
     * <p>
     * <b>Es la variante que necesita todo cargo que hereda el impuesto de la linea
     * en vez de traerlo en el comando.</b> Hoy solo el excedente: es mas consumo
     * del mismo articulo contratado, asi que su tratamiento fiscal <b>es el de su
     * linea</b> y no uno propio. El alta general no la usa porque alli el
     * tratamiento y la tarifa vienen en el cuerpo de la peticion, que es quien
     * decide.
     *
     * <p>
     * <b>Devuelve los dos campos fiscales juntos, y por eso el tipo de retorno es
     * un VO y no la tarifa suelta.</b> El par {@code (tax_treatment, tax_rate)}
     * solo es valido como par —{@code TAXED} con tarifa cero es inconstruible aguas
     * abajo— y separarlos es como aparecio el defecto que esto arregla: se tomo el
     * precio del excedente y se inventaron el impuesto.
     *
     * <p>
     * <b>Consulta aparte de {@link #findChargeModeInCompany} a proposito.</b>
     * Aquella sigue leyendo solo {@code charge_mode}: derivarla de esta le anadiria
     * la validacion de coherencia fiscal a un camino —el alta general— que hoy no
     * la hace, y una linea con un par incoherente heredado pasaria de devengar a
     * fallar sin que nadie lo hubiera pedido.
     *
     * <p>
     * Mismas exclusiones que {@link #findChargeModeInCompany}: el par
     * {@code (company_id, id)} de la FK compuesta y nada mas, sin mirar
     * {@code enabled}, ni la vigencia, ni el estado del contrato.
     *
     * @return vacio si la linea no existe o es de otra empresa
     */
    Optional<SubscriptionItemBillingProfile> findBillingProfileInCompany(Long subscriptionItemId,
            Long companyId);

    /**
     * {@code true} si la linea de contrato existe <b>y es de esa empresa</b>.
     *
     * <p>
     * <b>Existencia, no permiso de cobro.</b> Una linea {@code TRIAL} devuelve
     * {@code true} aqui y no debe generar ni un peso: quien vaya a devengar usa
     * {@link #findChargeModeInCompany}. Se mantiene porque la comprobacion de FK
     * sigue teniendo sentido por si sola.
     */
    default boolean existsInCompany(Long subscriptionItemId, Long companyId) {
        return findChargeModeInCompany(subscriptionItemId, companyId).isPresent();
    }
}
