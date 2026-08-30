package com.vetsoftware.app.aiproposal.infrastructure.persistence;

import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.PackOffer;
import com.vetsoftware.app.aiproposal.domain.PriceLadder;
import com.vetsoftware.app.aiproposal.domain.PriceTier;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.SellableItem;
import com.vetsoftware.app.aiproposal.domain.SellableItemKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * El unico fichero de la rodaja que conoce las tablas del catalogo comercial de
 * {@code catalogitem} y {@code pricelist}.
 *
 * <p>
 * ⛔ <strong>LEE LA ESCALERA ENTERA, no el tramo de entrada.</strong> Los cuatro
 * adaptadores que ya existen sobre estas tablas
 * ({@code JpaPublicCatalogQueryPort}, {@code JpaPublicPlanQueryPort},
 * {@code JpaCatalogQueryPorts}) filtran por {@code p.tier_min = 1}, y hacen
 * bien: publican un precio "desde" de portada y la escalera completa es
 * politica comercial que no se publica. <strong>Aqui se cotiza</strong>, y
 * cotizar con el precio "desde" es el defecto D-66 otra vez: trece unidades de
 * {@code EXTRA_USER} a 12.000 dan 156.000 cuando la respuesta son 141.000. Por
 * eso el {@code JOIN} de {@link #SQL_ITEM_TIERS} <em>no</em> lleva ese filtro y
 * la aritmetica la hace {@link PriceLadder}, en el dominio y sin base de datos.
 *
 * <p>
 * <strong>Queda fuera de {@code ADAPTADOR_JPA_CON_RODAJA}</strong>, que solo
 * alcanza a los {@code Jpa<Algo>Repository}: esta clase termina en
 * {@code QueryPort} y la regla no la mira. Su SQL necesita por tanto una rodaja
 * escrita a mano contra MySQL real —{@code SellableCatalogQueryPortIT}— o no lo
 * ejecutaria nadie hasta produccion, que es exactamente como sobrevivio meses
 * el defecto de la incidencia #196.
 *
 * <p>
 * <strong>Ni un literal booleano llegando como {@code Boolean}.</strong> MySQL
 * entrega {@code TINYINT} como {@code Byte} y el {@code CASE} como entero; los
 * convierte {@link #asBoolean(Object)} en Java, donde el tipo se ve. Es la
 * leccion de #196 y #472 aplicada a un adaptador que ninguna regla vigila,
 * porque las dos miran metodos anotados con {@code @Query} y aqui se usa
 * {@code createNativeQuery}.
 *
 * <p>
 * <strong>Sin {@code company_id} en ninguna parte, y no por disciplina sino
 * porque no existe</strong>: las cuatro tablas son catalogo global de
 * plataforma. Solo lee.
 */
@Component
public class JpaSellableCatalogQueryPort implements SellableCatalogQueryPort {

    private static final Logger log = LoggerFactory.getLogger(JpaSellableCatalogQueryPort.class);

    /**
     * La tarifa vigente. {@code valid_from}/{@code valid_to} se comparan contra la
     * fecha que entra por parametro y no contra {@code CURRENT_DATE}: el reloj lo
     * pone el {@link Clock} inyectado, que es lo unico que un test puede fijar.
     */
    private static final String SQL_PUBLISHED_LIST = """
            SELECT pl.id
              FROM price_lists pl
             WHERE pl.status = 'PUBLISHED'
               AND pl.enabled = TRUE
               AND pl.valid_from <= :hoy
               AND (pl.valid_to IS NULL OR pl.valid_to >= :hoy)
             ORDER BY pl.valid_from DESC, pl.id DESC
            """;

    /**
     * Una fila por <strong>tramo</strong>, no por articulo. Las nueve primeras
     * columnas se repiten en todos los tramos del mismo articulo y se agrupan en
     * Java; las cinco ultimas son la escalera.
     *
     * <p>
     * <strong>El {@code status} no filtra, se proyecta.</strong> Un articulo en
     * borrador o retirado tiene que llegar al motor para que produzca
     * {@code NOT_SELLABLE} en vez de {@code UNKNOWN_CODE}: son dos veredictos
     * distintos y juntos son la senal con la que se mide la calidad del modelo.
     * Hacia fuera no se distinguen (plan S4.2.3), pero eso lo decide la capa web,
     * no esta consulta.
     *
     * <p>
     * <strong>El penultimo {@code CASE} es el gate del autoservicio</strong>, y es
     * <em>literalmente</em> la rama del componente de
     * {@code JpaCatalogQueryPorts.SQL_PUBLISHED_ID_BY_CODE}: o el articulo es un
     * {@code BUNDLE}, o cuelga de algun {@code BUNDLE} {@code ACTIVE} publicado. Si
     * divergiera de aquel, la propuesta cotizaria lineas que la contratacion
     * rechaza despues —en el paso 6, cuando el prospecto ya se registro y verifico
     * el correo— con un texto deliberadamente indistinguible que no le dice
     * siquiera que linea sobra. Hoy los cuatro {@code EXTRA_*} dan {@code false}
     * por este {@code EXISTS}: la semilla 309 no mete ninguno en los tres packs.
     */
    private static final String SQL_ITEM_TIERS = """
            SELECT ci.code,
                   ci.name,
                   ci.short_description,
                   ci.item_type,
                   ci.is_core,
                   CASE WHEN ci.status = 'ACTIVE' THEN 1 ELSE 0 END,
                   CASE WHEN ci.item_type = 'BUNDLE'
                             OR EXISTS (SELECT 1
                                          FROM bundle_components bc
                                          JOIN catalog_items b ON b.id = bc.bundle_item_id
                                         WHERE bc.component_item_id = ci.id
                                           AND bc.enabled = TRUE
                                           AND b.enabled = TRUE
                                           AND b.item_type = 'BUNDLE'
                                           AND b.status = 'ACTIVE')
                        THEN 1 ELSE 0 END,
                   CASE WHEN ci.trial_eligibility = 'ELIGIBLE'
                        THEN ci.default_trial_days END,
                   pl.currency,
                   p.tier_min,
                   p.tier_max,
                   p.included_quantity,
                   p.unit_amount,
                   p.tax_rate
              FROM catalog_items ci
              JOIN catalog_prices p
                    ON p.catalog_item_id = ci.id
                   AND p.price_list_id   = :priceListId
                   AND p.billing_cycle   = :billingCycle
                   AND p.enabled = TRUE
              JOIN price_lists pl ON pl.id = p.price_list_id
             WHERE ci.enabled = TRUE
             ORDER BY ci.sort_order, ci.id, p.tier_min
            """;

    /**
     * ⛔ <strong>Solo componentes {@code MODULE}, y ese {@code AND} es la correccion
     * de S1.5 escrita en SQL.</strong> {@code CAPACITY_TERMINAL} es componente de
     * los tres paquetes y no entra al carrito por si solo, asi que con los
     * {@code CAPACITY} dentro la contencion "el paquete esta contenido en el
     * carrito" no se cumplia <em>nunca</em> y la funcion estrella de la v1 era
     * codigo muerto.
     *
     * <p>
     * <strong>{@code CORE} SI entra, y de el depende que la comparacion
     * cuadre.</strong> {@code 308:86} lo declara {@code item_type = 'MODULE'} y
     * {@code 309:240} lo mete en {@code PACK_CLINIC}: sin el, los modulos sueltos
     * del ejemplo del plan suman 155.000 —menos que los 189.000 del paquete— y la
     * oferta no saldria nunca. Con el suman los 224.000 que el plan publica, y el
     * paquete ahorra 35.000.
     */
    private static final String SQL_PACK_MODULE_COMPONENTS = """
            SELECT b.code, c.code
              FROM bundle_components bc
              JOIN catalog_items b ON b.id = bc.bundle_item_id
              JOIN catalog_items c ON c.id = bc.component_item_id
             WHERE bc.enabled = TRUE
               AND b.enabled = TRUE
               AND b.item_type = 'BUNDLE'
               AND b.status = 'ACTIVE'
               AND c.enabled = TRUE
               AND c.status = 'ACTIVE'
               AND c.item_type = 'MODULE'
             ORDER BY b.code, c.sort_order, c.id
            """;

    /**
     * Los arcos {@code REQUIRES} y solo esos. Los {@code RECOMMENDS} no entran a
     * proposito: auto-anadir un recomendado es un upsell disfrazado de requisito
     * tecnico, y al no existir el arco el cierre no puede seguirlo ni por accidente
     * el dia que alguien toque el bucle.
     *
     * <p>
     * Mismo {@code WHERE} que {@code JpaPublicCatalogQueryPort.SQL_REQUIREMENTS},
     * predicado a predicado: aquel es el grafo que la portada anuncia y este el que
     * el carrito cierra. Si publicaran predicados distintos, la portada prometeria
     * un grafo y la cotizacion aplicaria otro.
     */
    private static final String SQL_REQUIRES = """
            SELECT ci.code, req.code
              FROM catalog_item_dependencies d
              JOIN catalog_items ci  ON ci.id  = d.catalog_item_id
              JOIN catalog_items req ON req.id = d.related_item_id
             WHERE d.relation_type = 'REQUIRES'
               AND d.enabled = TRUE
               AND ci.status = 'ACTIVE'
               AND ci.enabled = TRUE
               AND req.status = 'ACTIVE'
               AND req.enabled = TRUE
             ORDER BY ci.sort_order, ci.id, req.sort_order, req.id
            """;

    private static final String SQL_ITEM_IDS = """
            SELECT ci.code, ci.id
              FROM catalog_items ci
             ORDER BY ci.sort_order, ci.id
            """;

    private final EntityManager entityManager;

    private final Clock clock;

    /**
     * Guarda del aviso de «sin tarifa publicada»: una sola vez por proceso. Ver
     * {@link #avisarUnaVezDeQueNoHayTarifa()}. No necesita volatilidad: repetir el
     * aviso una vez mas por una carrera entre hilos es inocuo, y sincronizarlo
     * costaria mas que el problema que evita.
     */
    private boolean avisadoSinTarifa;

    public JpaSellableCatalogQueryPort(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    public Optional<Long> findPublishedPriceListId() {
        Query query = entityManager.createNativeQuery(SQL_PUBLISHED_LIST)
                .setParameter("hoy", LocalDate.now(clock)).setMaxResults(1);
        List<?> filas = query.getResultList();
        if (filas.isEmpty()) {
            avisarUnaVezDeQueNoHayTarifa();
            return Optional.empty();
        }
        return Optional.of(((Number) filas.get(0)).longValue());
    }

    /**
     * &#9940; <strong>El estado en el que el asistente no puede cotizar NADA, y que
     * hasta hoy no se declaraba en ninguna parte.</strong>
     *
     * <p>
     * <strong>La cadena.</strong> El changeset 311 solo publica
     * {@code LISTA-2026-01} {@code AND EXISTS (SELECT 1 FROM system_users WHERE
     * enabled = TRUE)}, porque {@code chk_price_lists_published} exige una firma
     * humana y nominal y el propio changeset se niega —con razon— a inventar una
     * cuenta tecnica para que "funcione". {@code 008_create_system_users} solo crea
     * la tabla, y la semilla de laboratorio (262) va en
     * {@code context="local,e2e"}, que los perfiles de produccion y de test filtran
     * fuera. Sobre una base recien migrada, por tanto: cero cuentas de sistema, la
     * tarifa se queda en {@code DRAFT}, esta consulta devuelve vacio y el asistente
     * responde 200 con cero lineas <strong>a todos los prospectos</strong>.
     *
     * <p>
     * <strong>Lo que estaba documentado y lo que no.</strong> El changeset 382
     * declara por escrito su degradacion: sin {@code system_users} no siembra hints
     * y la feature nace "muda" —un estado legitimo, con su
     * {@code GenerationOutcome} ({@code DEGRADED_NO_HINTS}) y su etiqueta de
     * metrica—, y el carrito determinista sigue saliendo. El 311 documenta que el
     * alta de empresas queda bloqueada. <strong>Lo que nadie escribio es que la
     * misma condicion deja al asistente sin poder cotizar en absoluto</strong>, que
     * es peor que mudo: mudo responde el nucleo y el precio; esto no responde ni
     * una linea.
     *
     * <p>
     * <strong>Por que esto es un log y no un changeset.</strong> Cualquier
     * migracion que desbloquee una base nueva tendria que sembrar una cuenta de
     * sistema, y eso es exactamente el rastro de auditoria falso que 311 prohibe
     * —la firma de una tarifa tiene que poder atribuirse a una persona—. Ademas no
     * hay ningun {@code context} que separe produccion del contenedor de tests (los
     * dos corren con {@code contexts: production}), asi que un changeset que
     * arregle el primero publicaria tambien {@code LISTA-2026-01} en el contenedor
     * de Testcontainers y repuntaria alli {@code platform_billing_config},
     * cambiando el suelo de precios bajo 2 220 pruebas de integracion. La decision
     * —a quien se atribuye la firma— es del dueno de la plataforma; lo que si es
     * responsabilidad de este codigo es <strong>no callarselo</strong>.
     *
     * <p>
     * <strong>Una vez por proceso.</strong> Es un endpoint publico: repetir el
     * aviso en cada peticion lo convertiria en ruido y enseñaria a ignorar el
     * canal. El recuento por peticion ya vive en
     * {@code ai_proposal_generated_total} con {@code ai_outcome="no_catalog"}; esto
     * es el mensaje que le dice a quien lee el log <em>que hacer</em>.
     */
    private void avisarUnaVezDeQueNoHayTarifa() {
        if (avisadoSinTarifa)
            return;
        avisadoSinTarifa = true;
        log.warn("No hay ninguna lista de precios PUBLISHED vigente: el asistente comercial"
                + " responde sin una sola linea a todos los prospectos. En una base recien"
                + " migrada esto es lo esperado -el changeset 311 no publica LISTA-2026-01 si"
                + " no existe ningun system_user habilitado con el que firmarla-. Se resuelve"
                + " publicando la tarifa desde la consola de plataforma con una cuenta real."
                + " Se avisa una sola vez por proceso; el recuento por peticion vive en"
                + " ai_proposal_generated_total con ai_outcome=no_catalog");
    }

    /**
     * El id de {@code catalog_items} por codigo. Sin {@code status} ni
     * {@code enabled} en el {@code WHERE}: aqui no se decide que se vende -eso lo
     * decidio {@link #loadCatalog}- sino a que fila apunta la FK de una linea ya
     * cotizada, y una linea de un articulo retirado tiene que poder resolverla
     * igual.
     */
    @Override
    public Map<String, Long> findItemIdsByCode() {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (Object fila : entityManager.createNativeQuery(SQL_ITEM_IDS).getResultList()) {
            Object[] columnas = (Object[]) fila;
            ids.put(asString(columnas[0]), ((Number) columnas[1]).longValue());
        }
        return Map.copyOf(ids);
    }

    @Override
    public Optional<SellableCatalog> loadCatalog(Long priceListId,
            ProposalBillingCycle billingCycle) {
        if (priceListId == null || billingCycle == null)
            return Optional.empty();
        Map<String, SellableItem> items = leerArticulos(priceListId, billingCycle);
        if (items.isEmpty())
            return Optional.empty();
        Map<String, Set<String>> componentes = leerComponentesDePaquete();
        return Optional.of(new SellableCatalog(items, leerRequisitos(),
                construirPaquetes(items, componentes)));
    }

    /**
     * Agrupa los tramos por articulo y resuelve cada escalera. La agrupacion
     * depende del {@code ORDER BY} de la consulta y de nada mas: un
     * {@code LinkedHashMap} conserva el orden de presentacion del catalogo, que es
     * lo que hace reproducible el bloque del prompt y con el
     * {@code catalog_snapshot_hash}.
     */
    private Map<String, SellableItem> leerArticulos(Long priceListId,
            ProposalBillingCycle billingCycle) {
        Query query = entityManager.createNativeQuery(SQL_ITEM_TIERS)
                .setParameter("priceListId", priceListId)
                .setParameter("billingCycle", billingCycle.name());

        Map<String, List<PriceTier>> escaleras = new LinkedHashMap<>();
        Map<String, Object[]> cabeceras = new LinkedHashMap<>();
        for (Object fila : query.getResultList()) {
            Object[] columnas = (Object[]) fila;
            String code = asString(columnas[0]);
            cabeceras.putIfAbsent(code, columnas);
            escaleras.computeIfAbsent(code, ignorado -> new ArrayList<>())
                    .add(new PriceTier(asInt(columnas[9]), asInteger(columnas[10]),
                            asInt(columnas[11]), asAmount(columnas[12]), asAmount(columnas[13])));
        }

        Map<String, SellableItem> items = new LinkedHashMap<>();
        cabeceras.forEach((code, columnas) -> construir(code, columnas, escaleras.get(code))
                .ifPresent(item -> items.put(code, item)));
        return items;
    }

    /**
     * Un articulo cuya escalera no cierra <strong>no se cotiza</strong>: se omite
     * del catalogo y su codigo acaba como {@code UNKNOWN_CODE}, indistinguible
     * hacia fuera del resto de rechazos. Es la unica salida honesta —cotizar con
     * una escalera rota es cobrar mal— y ademas es un dato del que hay que
     * enterarse, por eso queda en el log de la plataforma y no en silencio.
     */
    private Optional<SellableItem> construir(String code, Object[] columnas,
            List<PriceTier> tramos) {
        String currency = asString(columnas[8]);
        try {
            PriceLadder escalera = new PriceLadder(code, tramos, currency);
            return Optional.of(new SellableItem(code, asString(columnas[1]), asString(columnas[2]),
                    asKind(columnas[3]), asBoolean(columnas[4]), asBoolean(columnas[5]),
                    asBoolean(columnas[6]), asTrialDays(columnas[7]), escalera.unitAmountForOne(),
                    escalera.taxRate(), currency));
        } catch (IllegalArgumentException rota) {
            log.warn("Articulo {} excluido del catalogo de propuestas: {}", code,
                    rota.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Set<String>> leerComponentesDePaquete() {
        Map<String, Set<String>> componentes = new LinkedHashMap<>();
        for (Object fila : entityManager.createNativeQuery(SQL_PACK_MODULE_COMPONENTS)
                .getResultList()) {
            Object[] columnas = (Object[]) fila;
            componentes.computeIfAbsent(asString(columnas[0]), ignorado -> new LinkedHashSet<>())
                    .add(asString(columnas[1]));
        }
        return componentes;
    }

    private Map<String, List<String>> leerRequisitos() {
        Map<String, List<String>> arcos = new LinkedHashMap<>();
        for (Object fila : entityManager.createNativeQuery(SQL_REQUIRES).getResultList()) {
            Object[] columnas = (Object[]) fila;
            arcos.computeIfAbsent(asString(columnas[0]), ignorado -> new ArrayList<>())
                    .add(asString(columnas[1]));
        }
        return arcos;
    }

    /**
     * Los paquetes vendibles con sus modulos. Un {@code BUNDLE} sin ni un
     * componente {@code MODULE} se construye igual y {@code PackOffer.esComparable}
     * lo descarta despues: la contencion de un conjunto vacio es cierta por
     * vacuidad y ese paquete se ofreceria <em>siempre</em>, incluso con el carrito
     * vacio.
     */
    private static List<PackOffer> construirPaquetes(Map<String, SellableItem> items,
            Map<String, Set<String>> componentes) {
        List<PackOffer> paquetes = new ArrayList<>();
        items.values().stream().filter(item -> item.kind() == SellableItemKind.BUNDLE)
                .filter(SellableItem::esCotizable)
                .forEach(pack -> paquetes.add(new PackOffer(pack.code(), pack.name(),
                        pack.unitAmount(), pack.taxRate(), pack.trialDays(),
                        componentes.getOrDefault(pack.code(), Set.of()))));
        return paquetes;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static int asInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    /**
     * {@code NULL} en la columna de prueba significa "no concede ninguna", que es
     * el lado seguro: {@code 0} no regala nada. Un negativo en base solo puede ser
     * dato corrupto y {@link SellableItem} lo rechazaria; se normaliza aqui para
     * que un articulo no tumbe el catalogo entero.
     */
    private static int asTrialDays(Object value) {
        return value == null ? 0 : Math.max(0, ((Number) value).intValue());
    }

    private static BigDecimal asAmount(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        return value instanceof BigDecimal decimal
                ? decimal
                : new BigDecimal(String.valueOf(value));
    }

    /**
     * MySQL entrega {@code TINYINT} como {@code Byte} y el {@code CASE … THEN 1
     * ELSE 0} como entero; ninguno de los dos es un {@code Boolean} y nadie los
     * convierte solo. Es el defecto de #472 —que tumbo el alta de empresa entera—
     * evitado por construccion: aqui no hay proyeccion automatica a la que pedirle
     * el milagro.
     */
    private static boolean asBoolean(Object value) {
        if (value == null)
            return false;
        if (value instanceof Boolean flag)
            return flag;
        return ((Number) value).intValue() != 0;
    }

    /**
     * El enum de {@code catalogitem} no se importa: el dominio de una feature nunca
     * conoce el de otra. Lo que cruza es la cadena de la columna, y este metodo es
     * el unico sitio donde las dos representaciones se miran.
     */
    private static SellableItemKind asKind(Object value) {
        return SellableItemKind.valueOf(String.valueOf(value));
    }
}
