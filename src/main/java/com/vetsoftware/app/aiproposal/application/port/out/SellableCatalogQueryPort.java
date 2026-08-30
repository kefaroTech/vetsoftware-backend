package com.vetsoftware.app.aiproposal.application.port.out;

import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import java.util.Optional;

/**
 * La foto del catalogo comercial con la que esta rodaja decide todo: lo que el
 * prompt le ensena al modelo, lo que el motor determinista valida y lo que se
 * cotiza.
 *
 * <p>
 * ⛔ <strong>NI UN METODO puede llevar {@code companyId} ni {@code Company} en
 * el nombre</strong> (plan S5.3), ni siquiera uno que nadie use desde aqui. El
 * disparador de {@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}
 * ({@code VetSoftwareConditions:571-573}) no es el nombre del caso de uso: le
 * basta con que el <em>puerto</em> declare algun metodo con esa firma para
 * empezar a exigir {@code hasRole('SYSTEM')} a secas a todo el que lo consuma —
 * y estos casos de uso son anonimos, sin un solo {@code @PreAuthorize} que
 * ensenar. El catalogo comercial es global de plataforma
 * ({@code catalog_items}, {@code catalog_prices}, {@code bundle_components} y
 * {@code price_lists} no alcanzan {@code companies} por ninguna asociacion),
 * asi que la restriccion no cuesta nada: no hay empresa de la que tirar.
 *
 * <p>
 * <strong>Es el puerto PROPIO de {@code aiproposal}, no el de
 * {@code pricelist}.</strong> Importar {@code PublicCatalogQueryPort} desde
 * {@code application} rompe el vertical slicing; el unico fichero de todo el
 * proyecto que puede conocer las dos representaciones es su adaptador.
 */
public interface SellableCatalogQueryPort {

    /**
     * La tarifa publicada y vigente hoy. Vacio es un estado normal y no un error:
     * {@code 310_seed_price_list_2026} siembra la lista en {@code DRAFT} a
     * proposito y {@code 311} solo la publica si existe un {@code system_users}
     * real, que en una base recien migrada no existe. Sin tarifa no se puede
     * cotizar nada, y el caso de uso tiene que degradar en vez de inventar precios.
     */
    Optional<Long> findPublishedPriceListId();

    /**
     * El catalogo entero de esa tarifa y ese ciclo, con {@code unitAmount}
     * <strong>ya resuelto contra la escalera completa</strong>.
     *
     * <p>
     * Trae tambien los articulos que <em>no</em> se pueden vender —en borrador,
     * retirados, o vivos pero fuera del autoservicio—, porque sin ellos el motor no
     * puede distinguir "ese codigo no existe" de "existe y no se contrata", que son
     * veredictos distintos y la senal con la que se mide si el modelo sirve. Esa
     * distincion es interna: hacia fuera los cinco veredictos son indistinguibles.
     */
    Optional<SellableCatalog> loadCatalog(Long priceListId, ProposalBillingCycle billingCycle);

    /**
     * El id de {@code catalog_items} por codigo, que es lo que
     * {@code chk_ai_proposal_lines_resolved} exige de toda linea aceptada: la FK
     * resuelta o el veredicto distinto de {@code ACCEPTED}.
     *
     * <p>
     * <strong>Va aparte de {@link SellableCatalog} y no como un campo mas de
     * {@code SellableItem}</strong>: el catalogo entra en el prompt, y un id
     * interno en el contexto del modelo es un dato que no le sirve para nada y que
     * puede repetir en su prosa. El motor determinista razona con codigos; la clave
     * ajena la resuelve la capa que escribe.
     */
    java.util.Map<String, Long> findItemIdsByCode();
}
