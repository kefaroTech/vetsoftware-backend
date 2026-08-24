package com.vetsoftware.app.subscriptionbilling.application.port.out;

/**
 * La FK compuesta {@code fk_subscription_charges_item} sobre
 * {@code (company_id, subscription_item_id)}, comprobada antes de construir el
 * cargo.
 *
 * <p>
 * <b>Es un {@code ValidationPort} y no un {@code QueryPort} porque este slice
 * no lee ni un campo de la linea del contrato</b>: el cargo congela su propia
 * descripcion, su cantidad y su tarifa en el momento de devengarse. Traer aqui
 * un {@code SubscriptionItemRef} seria copiar datos que nadie usa y atar la
 * capa de dinero a la forma de {@code SubscriptionItemJpaEntity}. Es el caso
 * que el {@code CLAUDE.md} describe como «no necesitas datos del agregado
 * externo, solo el ID».
 *
 * <p>
 * <b>Solo declara la variante acotada por empresa, y eso es deliberado.</b> La
 * forma ancha —{@code existsById(itemId)}— no rechazaria nada util: permitiria
 * colgar el cargo de la clinica A de una linea del contrato de la clinica B. La
 * base lo rechaza igual por la FK compuesta, pero como una violacion de
 * constraint convertida en 500 a mitad del cierre mensual; aqui devuelve
 * {@code false} y el caso de uso dice cual de los cinco ids del cuerpo estaba
 * mal. Ese razonamiento ya estaba escrito para el contrato en
 * {@code JpaSubscriptionQueryPort}; faltaba aplicarlo a las otras dos
 * referencias.
 *
 * <p>
 * Devuelve un booleano en vez de lanzar: la excepcion de FK inexistente la
 * decide el caso de uso, nunca el adaptador.
 */
public interface SubscriptionItemValidationPort {

    /**
     * {@code true} si la linea de contrato existe <b>y es de esa empresa</b>.
     *
     * <p>
     * Comprueba exactamente lo que comprueba la FK compuesta —el par
     * {@code (company_id, id)}— y nada mas. En concreto <b>no</b> mira
     * {@code enabled} ni la vigencia: devengar el cargo de un periodo ya cerrado
     * contra la linea que lo presto es legitimo, y ser mas estricto que la base
     * rechazaria cierres correctos.
     */
    boolean existsInCompany(Long subscriptionItemId, Long companyId);
}
