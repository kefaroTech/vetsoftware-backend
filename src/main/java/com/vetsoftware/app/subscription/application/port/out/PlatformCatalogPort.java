package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.InitialCapacityTemplate;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.util.List;
import java.util.Optional;

/**
 * Resuelve el minimo estructural de la plataforma para poder firmar un contrato
 * inicial.
 *
 * <p>
 * Devuelve {@link Optional#empty()} —no lanza— cuando falta cualquiera de las
 * cinco piezas: el articulo {@code CORE} activo, su enlace a un submodulo, la
 * lista de precios publicada por defecto, el precio de ese articulo para el
 * ciclo pedido, y la fila de configuracion de plataforma. Quien decide que
 * hacer con la ausencia es el caso de uso, no el adaptador: aqui la respuesta
 * correcta a «no hay catalogo» es «no hay», no una excepcion.
 *
 * <p>
 * {@code platform_billing_config}, {@code price_lists}, {@code catalog_items} y
 * {@code catalog_prices} son <strong>catalogo global de plataforma</strong>,
 * sin {@code company_id}: no hay empresa por la que acotar y
 * {@code REFERENCIAS_CROSS_FEATURE_ACOTADAS_POR_EMPRESA} no aplica.
 */
public interface PlatformCatalogPort {
    Optional<InitialContractTemplate> findInitialContractTemplate(BillingCycle billingCycle);

    /**
     * Las capacidades que el minimo estructural concede: los articulos
     * {@code CAPACITY} marcados {@code is_core}, con su tramo publicado para el
     * ciclo pedido.
     *
     * <p>
     * <strong>Sin esto un contrato inicial no abre nada que se pueda
     * contar</strong> y la empresa nace sin poder crear ni su primera sede ni su
     * primer usuario, que es exactamente el estado de corte total que el modelo
     * prohibe (#490). El nucleo es un {@code MODULE} y no puede llevar unidad —
     * {@code chk_catalog_items_capacity_unit} lo prohibe en los dos sentidos—, asi
     * que la capacidad tiene que venir de sus propios articulos: no hay forma de
     * colgarla del {@code CORE}.
     *
     * <p>
     * Lista vacia —no excepcion— cuando el catalogo no tiene ninguno. Quien decide
     * si eso basta es el caso de uso, que es el unico que sabe que unidades
     * necesita una empresa para terminar de nacer
     * ({@code StructuralCapacityMinimum}).
     */
    List<InitialCapacityTemplate> findInitialCapacityTemplates(BillingCycle billingCycle);

    /**
     * Los dias de gracia que la plataforma concede por defecto
     * ({@code platform_billing_config.default_grace_days}).
     *
     * <p>
     * Existe aparte de {@link #findInitialContractTemplate} porque el alta por API
     * necesita <strong>solo este numero</strong> y no las cinco piezas del minimo
     * estructural: exigirle el catalogo entero para saber cuantos dias de gracia
     * dar seria negarle el alta a un contrato que no lo necesita.
     *
     * <p>
     * {@link Optional#empty()} cuando la fila de configuracion no existe. Quien
     * decide que hacer con la ausencia es el caso de uso; aqui «no hay» es «no
     * hay».
     */
    Optional<Integer> findDefaultGraceDays();
}
