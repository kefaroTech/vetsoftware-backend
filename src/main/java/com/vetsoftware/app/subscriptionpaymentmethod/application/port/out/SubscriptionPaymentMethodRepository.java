package com.vetsoftware.app.subscriptionpaymentmethod.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <strong>No existe ningun {@code findById(Long)} ancho, y es
 * deliberado.</strong> {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca
 * al caso de uso que conoce la variante ancha y no la acotada; la forma de no
 * poder equivocarse es que la ancha no exista. Toda lectura por id de este
 * slice lleva la empresa.
 */
public interface SubscriptionPaymentMethodRepository {

    SubscriptionPaymentMethod save(SubscriptionPaymentMethod paymentMethod);

    Optional<SubscriptionPaymentMethod> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Le quita la marca de predeterminado al medio vigente de la empresa, si lo
     * hay, excluyendo el que se esta marcando.
     *
     * <p>
     * <strong>Es una escritura directa y no un {@code save}, y el motivo es el
     * orden.</strong> Hibernate decide cuando vacia sus dos {@code UPDATE} al
     * flush, y {@code uq_subscription_payment_methods_default} se comprueba <em>por
     * instruccion</em>, no al final de la transaccion. Si el motor viera primero el
     * que marca el nuevo predeterminado, encontraria dos {@code default_marker}
     * iguales y rechazaria la operacion. Ejecutando la limpieza en el acto, la
     * ventana no existe.
     *
     * @return cuantas filas perdieron la marca; cero cuando no habia predeterminado
     *         vigente, que es un caso normal y no un error
     */
    int clearDefaultForCompany(Long companyId, Long excludedId);

    /**
     * Medio ya registrado con el mismo testigo de pasarela.
     *
     * <p>
     * <strong>Sin filtro de empresa, y no es una fuga:</strong>
     * {@code uq_subscription_payment_methods_token} es una unicidad <em>global</em>
     * sobre {@code (gateway, token)}, asi que la unica forma de saber si un testigo
     * ya esta tomado es preguntar sin acotar. Devuelve una fila como maximo —no es
     * un listado— y el caso de uso comprueba la empresa antes de exponer nada: si
     * el testigo es de otra clinica, rechaza con un 409 <em>sin revelar de
     * quien</em>. Mismo criterio que {@code findByGatewayAndGatewayReference} en
     * {@code SubscriptionPaymentRepository}.
     */
    Optional<SubscriptionPaymentMethod> findByGatewayAndToken(String gateway, String token);

    PageResult<SubscriptionPaymentMethod> findAllByCompanyId(Long companyId, int page,
            int pageSize);

    /**
     * Barrido de plataforma cross-tenant: tarjetas con mandato vivo que caducan
     * antes de la fecha dada. Solo lo consume un puerto SYSTEM.
     */
    PageResult<SubscriptionPaymentMethod> findAllExpiringBefore(LocalDate before, int page,
            int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<SubscriptionPaymentMethod> findAll(int page, int pageSize);
}
