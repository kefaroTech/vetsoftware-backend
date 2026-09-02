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
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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
     * <strong>El penultimo {@code CASE} es el gate del autoservicio</strong>, y su
     * conjunto de aceptacion tiene que ser un <em>subconjunto</em> del de
     * {@code JpaCatalogQueryPorts.SQL_PUBLISHED_ID_BY_CODE}, que es el paso
     * vinculante: o el articulo es un {@code BUNDLE}, o es un {@code MODULE} o una
     * {@code CAPACITY} que cuelga de algun {@code BUNDLE} {@code ACTIVE} publicado.
     *
     * <p>
     * <strong>La restriccion por {@code item_type} no es adorno.</strong> Sin ella,
     * un {@code ONE_TIME} que fuera componente de un paquete saldria marcado como
     * contratable y la contratacion lo rechazaria despues, en el paso 6, cuando el
     * prospecto ya se registro y verifico el correo, con un texto deliberadamente
     * indistinguible que no le dice siquiera que linea sobra. La direccion del
     * error es lo que importa: quedarse corto pierde una venta y el prospecto se
     * entera antes; pasarse lo estrella despues. Por eso esta consulta se aprieta
     * contra la de {@code quote} y nunca al reves.
     *
     * <p>
     * <strong>Lo que este {@code CASE} NO replica es el
     * {@code p.tier_min = 1}</strong> que aquel exige en su {@code JOIN}, y no es
     * un olvido: {@code SQL_ITEM_TIERS} trae la escalera entera a proposito, asi
     * que aqui no hay un tramo unico al que atarse. La cobertura la da el dominio y
     * es mas fuerte que ese predicado. {@code PriceLadder} exige que el primer
     * tramo arranque en uno, y ademas que la escalera sea contigua y cierre;
     * {@code construir(...)} descarta del catalogo el articulo cuya escalera no
     * cumple. Un articulo sin tramo de entrada nunca llega a tener
     * {@code selfServiceEligible}: no llega al catalogo. El subconjunto se conserva
     * por esa via, no por esta columna.
     *
     * <p>
     * Hoy los cuatro {@code EXTRA_*} dan {@code false} por este {@code EXISTS}: la
     * semilla 309 no mete ninguno en los tres packs.
     */
    private static final String SQL_ITEM_TIERS = """
            SELECT ci.code,
                   ci.name,
                   ci.short_description,
                   ci.item_type,
                   ci.structural_minimum,
                   CASE WHEN ci.status = 'ACTIVE' THEN 1 ELSE 0 END,
                   CASE WHEN ci.item_type = 'BUNDLE'
                             OR (ci.item_type IN ('MODULE', 'CAPACITY')
                                 AND EXISTS (SELECT 1
                                               FROM bundle_components bc
                                               JOIN catalog_items b ON b.id = bc.bundle_item_id
                                              WHERE bc.component_item_id = ci.id
                                                AND bc.enabled = TRUE
                                                AND b.enabled = TRUE
                                                AND b.item_type = 'BUNDLE'
                                                AND b.status = 'ACTIVE'))
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
     * &#9940; <strong>Cada cuanto puede repetirse el aviso de «sin tarifa
     * publicada». Esto ERA «una sola vez por proceso», y el cambio es deliberado:
     * no lo devuelvas a un booleano pensando que ahorras ruido.</strong>
     *
     * <p>
     * <strong>Que fallaba.</strong> Con el aviso unico, un contenedor que lleva
     * dias arriba tiene el mensaje en el arranque y <em>no</em> en la ventana de la
     * peticion que falla. Quien depura mira los ultimos minutos de log, no
     * encuentra nada, y concluye que el asistente esta sano cuando lleva dias
     * respondiendo sin una sola linea a todos los prospectos. Ya costo un
     * diagnostico entero: el estado mas grave de esta rodaja era el unico invisible
     * en la ventana en la que se mira.
     *
     * <p>
     * <strong>El equilibrio que sustituye a aquello.</strong> El mensaje completo
     * sale como mucho una vez por ventana —un endpoint publico y anonimo bajo
     * trafico convertiria una linea por peticion en ruido que ensena a ignorar el
     * canal— y <strong>ninguna peticion se pierde</strong>: las que caen dentro de
     * la ventana se cuentan y el aviso siguiente dice cuantas fueron, asi que desde
     * cualquier linea se reconstruye el periodo entero. Cinco minutos es mas corto
     * que cualquier ventana con la que se mira un incidente y acota el volumen a
     * 288 lineas al dia en el peor caso.
     *
     * <p>
     * El recuento exacto y por peticion sigue viviendo en
     * {@code ai_proposal_generated_total} con {@code ai_outcome="no_catalog"}. Esto
     * no compite con esa serie: es el mensaje que dice <em>que hacer</em>, y tiene
     * que estar donde alguien lo va a leer.
     */
    private static final Duration VENTANA_DEL_AVISO = Duration.ofMinutes(5);

    /** Ver {@link #avisarDeQueNoHayTarifa()}. */
    private final AvisoPorVentana avisoSinTarifa = new AvisoPorVentana();

    /** Ver {@link #avisarDeQueElCatalogoEstaVacio(Long, ProposalBillingCycle)}. */
    private final AvisoPorVentana avisoCatalogoVacio = new AvisoPorVentana();

    /**
     * Ver
     * {@link #avisarDeQueNoHayNucleoCotizable(Long, ProposalBillingCycle, CatalogoLeido)}.
     */
    private final AvisoPorVentana avisoSinNucleo = new AvisoPorVentana();

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
            avisarDeQueNoHayTarifa();
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
     * <strong>Con rastro en la ventana de la peticion que falla, no solo en el
     * arranque.</strong> Ver {@link #VENTANA_DEL_AVISO}, donde esta escrito por que
     * esto dejo de ser «una sola vez por proceso» y que se puso en su lugar.
     */
    private void avisarDeQueNoHayTarifa() {
        Long silenciadas = avisoSinTarifa.tocaEscribir(clock.millis());
        if (silenciadas == null)
            return;
        log.warn("No hay ninguna lista de precios PUBLISHED vigente: el asistente comercial"
                + " responde sin una sola linea a todos los prospectos. En una base recien"
                + " migrada esto es lo esperado -el changeset 311 no publica LISTA-2026-01 si"
                + " no existe ningun system_user habilitado con el que firmarla-. Se resuelve"
                + " publicando la tarifa desde la consola de plataforma con una cuenta real."
                + " Otras {} peticiones dieron lo mismo desde el aviso anterior y no se"
                + " escribieron; este aviso no se repite antes de {} minutos. El recuento"
                + " exacto por peticion vive en ai_proposal_generated_total con"
                + " ai_outcome=no_catalog", silenciadas, VENTANA_DEL_AVISO.toMinutes());
    }

    /**
     * &#9940; <strong>La tarifa SI esta publicada y aun asi no hay nada que
     * vender.</strong> Este camino era mudo, y compartia desenlace con el de
     * arriba: la senal decia «publica la tarifa» a quien ya la tenia publicada, y
     * quien la recibia perdia el turno comprobando algo que estaba bien.
     *
     * <p>
     * <strong>Lo que hay que mirar cuando aparece</strong> son las tres cosas que
     * {@code SQL_ITEM_TIERS} exige a la vez y que este mensaje no puede adivinar:
     * que los articulos esten {@code ACTIVE} y {@code enabled}, que la lista tenga
     * tramos, y que los tenga <em>para el ciclo de facturacion pedido</em> —una
     * tarifa solo con tramos anuales deja mudo el asistente para quien pide
     * mensual, y esa asimetria no se ve mirando la tabla por encima—. Por eso el
     * mensaje lleva la lista y el ciclo: sin ellos hay que reproducirlo para saber
     * cual de los dos fallaba.
     */
    private void avisarDeQueElCatalogoEstaVacio(Long priceListId,
            ProposalBillingCycle billingCycle) {
        Long silenciadas = avisoCatalogoVacio.tocaEscribir(clock.millis());
        if (silenciadas == null)
            return;
        log.warn("La lista de precios {} esta PUBLISHED y vigente pero no cuelga de ella ni un"
                + " articulo vendible para el ciclo {}: el asistente comercial responde sin una"
                + " sola linea a todos los prospectos. OJO, NO es el mismo estado que 'no hay"
                + " tarifa publicada': aqui la tarifa ya esta bien y lo que hay que revisar es"
                + " el catalogo -articulos en ACTIVE y enabled, y con tramo de precio para ESE"
                + " ciclo-. Otras {} peticiones dieron lo mismo desde el aviso anterior y no se"
                + " escribieron; este aviso no se repite antes de {} minutos. El recuento"
                + " exacto por peticion vive en ai_proposal_generated_total con"
                + " ai_outcome=empty_catalog", priceListId, billingCycle, silenciadas,
                VENTANA_DEL_AVISO.toMinutes());
    }

    /**
     * La guarda de ruido compartida por los dos avisos de arriba. Ver
     * {@link #VENTANA_DEL_AVISO} para el porque de la ventana; esto es solo su
     * mecanica, escrita una vez para que los dos avisos no puedan divergir.
     */
    private static final class AvisoPorVentana {

        private final AtomicLong proximo = new AtomicLong(Long.MIN_VALUE);

        private final AtomicLong silenciadas = new AtomicLong();

        /**
         * @return cuantas peticiones se silenciaron desde el aviso anterior, si toca
         *         escribir; {@code null} si la ventana sigue abierta y esta peticion
         *         solo suma al contador
         */
        Long tocaEscribir(long ahora) {
            long previsto = proximo.get();
            // La ventana la abre UN solo hilo: el que gana el CAS escribe, los demas
            // suman al contador. Sin el CAS, un pico de peticiones simultaneas
            // escribiria una linea por hilo justo en el momento de mas trafico, que
            // es cuando menos falta hace.
            if (ahora < previsto
                    || !proximo.compareAndSet(previsto, ahora + VENTANA_DEL_AVISO.toMillis())) {
                silenciadas.incrementAndGet();
                return null;
            }
            return silenciadas.getAndSet(0);
        }
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
        CatalogoLeido leido = leerArticulos(priceListId, billingCycle);
        if (leido.items().isEmpty()) {
            avisarDeQueElCatalogoEstaVacio(priceListId, billingCycle);
            return Optional.empty();
        }
        Optional<SellableItem> nucleo = resolverNucleo(leido);
        if (nucleo.isEmpty()) {
            avisarDeQueNoHayNucleoCotizable(priceListId, billingCycle, leido);
            return Optional.empty();
        }
        Map<String, Set<String>> componentes = leerComponentesDePaquete();
        return Optional.of(new SellableCatalog(leido.items(), leerRequisitos(),
                construirPaquetes(leido.items(), componentes), nucleo.get()));
    }

    /**
     * &#9940; <strong>LA CAPA ANTICORRUPCION, Y LA UNICA LINEA DEL BACKEND DONDE
     * {@code structural_minimum} SIGNIFICA ALGO PARA EL ASISTENTE.</strong>
     *
     * <p>
     * <strong>{@code structural_minimum} es un bit compartido por dos contextos que
     * lo leen distinto, y las dos lecturas son correctas.</strong> Para el alta de
     * plataforma es un <em>predicado de conjunto</em> —«forma parte del minimo
     * estructural»— y
     * {@code PlatformCatalogTemplateJpaRepository.findInitialCapacityTemplates}
     * exige que lo lleven las dos capacidades ademas del modulo: si a alguien se le
     * ocurre «limpiarlas» en la semilla, {@code POST /api/v1/register} empieza a
     * devolver {@code PLATFORM_CATALOG_NOT_CONFIGURED} y el alta de la empresa
     * falla entera, que es un fallo peor (#490, y la semilla 308:41-49 lo grita en
     * mayusculas). Para esta rodaja, en cambio, el nucleo es <em>el articulo que
     * todo carrito arrastra</em>, y eso solo puede ser <strong>un modulo</strong>:
     * lo que el paso 3 del motor mete en el carrito es una linea cotizada, no una
     * cantidad.
     *
     * <p>
     * <strong>Por eso la traduccion vive aqui y no en el dominio.</strong> El
     * defecto de produccion fue precisamente que el bit cruzaba la frontera en
     * crudo: {@code SellableCatalog} resolvia el nucleo con un {@code findFirst()}
     * sobre los tres que lo llevan, sorteado por el orden de iteracion de un
     * {@code Map.copyOf} —que la JVM aleatoriza en cada arranque—, y cuando salia
     * una capacidad, que no es cotizable, el carrito quedaba vacio sin dejar
     * rastro. Estable dentro de un proceso y distinto tras reiniciar: se curaba
     * solo y volvia. Con la traduccion en la frontera, el dominio recibe el
     * concepto ya resuelto y no le queda nada que leer mal.
     *
     * <p>
     * <strong>Ordenado por codigo</strong>: si algun dia hubiera dos modulos
     * {@code structural_minimum} cotizables, todos los procesos elegirian el mismo.
     */
    private static Optional<SellableItem> resolverNucleo(CatalogoLeido leido) {
        return leido.nucleosDeclarados().stream().sorted().map(leido.items()::get)
                .filter(java.util.Objects::nonNull)
                .filter(item -> item.kind() == SellableItemKind.MODULE)
                .filter(SellableItem::esCotizable).findFirst();
    }

    /**
     * Lo que sale de {@code SQL_ITEM_TIERS}: los articulos ya construidos y,
     * aparte, los codigos que la columna {@code structural_minimum} marca.
     * <strong>El bit se queda en este record y no sigue hacia dentro</strong>;
     * quien lo traduce es {@link #resolverNucleo(CatalogoLeido)}.
     */
    private record CatalogoLeido(Map<String, SellableItem> items, Set<String> nucleosDeclarados) {
    }

    /**
     * &#9940; <strong>El tercer estado que dejaba mudo al asistente, y el que mas
     * caro salio.</strong> La tarifa esta publicada y el catalogo trae articulos
     * —los dos avisos de arriba callan— pero <strong>no hay un modulo
     * {@code structural_minimum} que se pueda cotizar</strong>, asi que el paso 3
     * de {@code ProposalCart} no tendria de donde partir y el cierre de
     * {@code REQUIRES} arrancaria de un carrito vacio. Lo que salia era un 200 con
     * {@code lines: []}, {@code discardedLines: 0} y todos los importes a cero: la
     * respuesta mas dificil de distinguir de «el modelo no entendio».
     *
     * <p>
     * <strong>Ya no sale eso.</strong> Devolver {@link Optional#empty()} enruta el
     * caso al desenlace que <em>ya existe</em> para «la tarifa esta publicada y no
     * se puede cotizar con ella»: {@code GenerateProposalService} responde
     * {@code ProposalViewDto.sinCatalogo()} —sin token y sin boton que lleve a un
     * callejon— y cuenta {@code ai_proposal_generated_total} con
     * {@code ai_outcome="empty_catalog"}, que tiene alerta critica
     * ({@code VetSoftwareAiProposalEmptyCatalog}) y procedimiento escrito. No se
     * inventa un modo de fallo nuevo: se encamina al que ya esta vigilado.
     *
     * <p>
     * &#9888; <strong>Lo que la alerta NO dice, y por eso este log
     * importa.</strong> Su resumen manda «anadir items a la lista de precios
     * vigente», y aqui items hay: lo que falta es el <em>modulo nucleo</em>. La
     * accion correcta esta en estas lineas, no en el runbook, hasta que la
     * descripcion de la alerta se amplie en {@code vetsoftware-infrastructure}.
     *
     * <p>
     * Enumera los {@code structural_minimum} que si llegaron porque el diagnostico
     * esta justamente ahi: si salen {@code CAPACITY_USER} y {@code CAPACITY_BRANCH}
     * y no {@code CORE}, lo que falta es la fila del modulo o su tramo de precio,
     * no la tarifa entera.
     */
    private void avisarDeQueNoHayNucleoCotizable(Long priceListId,
            ProposalBillingCycle billingCycle, CatalogoLeido leido) {
        Long silenciadas = avisoSinNucleo.tocaEscribir(clock.millis());
        if (silenciadas == null)
            return;
        log.warn("La lista de precios {} tiene articulos para el ciclo {} pero ninguno es un modulo"
                + " is_core cotizable, asi que el asistente comercial no puede cotizar nada y"
                + " responde como si el catalogo estuviera vacio (ai_outcome=empty_catalog). Los"
                + " is_core que si llegaron son {} -si ahi no esta CORE, revisa esa fila de"
                + " catalog_items (item_type MODULE, status ACTIVE, enabled) y su tramo en"
                + " catalog_prices para ESE ciclo; si esta pero no se cotiza, es que no cuelga de"
                + " ningun BUNDLE ACTIVE y por eso no es autoservicio-. OJO: la alerta"
                + " VetSoftwareAiProposalEmptyCatalog dira 'faltan items en la tarifa', y aqui"
                + " items hay: lo que falta es el modulo nucleo. Y NO desmarques is_core en"
                + " CAPACITY_USER ni CAPACITY_BRANCH para 'limpiar' la lista, que ese bit lo usa"
                + " findInitialCapacityTemplates como predicado de conjunto y el alta de empresas"
                + " empezaria a fallar con PLATFORM_CATALOG_NOT_CONFIGURED (#490). Otras {}"
                + " peticiones dieron lo mismo desde el aviso anterior y no se escribieron; este"
                + " aviso no se repite antes de {} minutos", priceListId, billingCycle,
                leido.nucleosDeclarados().stream().sorted().toList(), silenciadas,
                VENTANA_DEL_AVISO.toMinutes());
    }

    /**
     * Agrupa los tramos por articulo y resuelve cada escalera. La agrupacion
     * depende del {@code ORDER BY} de la consulta y de nada mas: un
     * {@code LinkedHashMap} conserva el orden de presentacion del catalogo, que es
     * lo que hace reproducible el bloque del prompt y con el
     * {@code catalog_snapshot_hash}.
     */
    private CatalogoLeido leerArticulos(Long priceListId, ProposalBillingCycle billingCycle) {
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
        Set<String> nucleosDeclarados = new LinkedHashSet<>();
        cabeceras.forEach((code, columnas) -> construir(code, columnas, escaleras.get(code))
                .ifPresent(item -> {
                    items.put(code, item);
                    // La columna structural_minimum llega como Byte desde MySQL y se queda aqui:
                    // el dominio no la ve. Ver resolverNucleo(...).
                    if (asBoolean(columnas[4]))
                        nucleosDeclarados.add(code);
                }));
        return new CatalogoLeido(items, nucleosDeclarados);
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
            // columnas[4] es structural_minimum y NO se pasa: lo recoge leerArticulos
            // aparte.
            return Optional.of(new SellableItem(code, asString(columnas[1]), asString(columnas[2]),
                    asKind(columnas[3]), asBoolean(columnas[5]), asBoolean(columnas[6]),
                    asTrialDays(columnas[7]), escalera.unitAmountForOne(), escalera.taxRate(),
                    currency));
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
