package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogAreaRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogRequirementRowDto;
import com.vetsoftware.app.pricelist.application.port.out.PublicCatalogQueryPort;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * SQL nativo por los dos motivos que ya escribio
 * {@link JpaPublicPlanQueryPort}: lo especificado como norma son la TABLA y sus
 * COLUMNAS —no los nombres de campo Java que elija {@code catalogitem}— y aqui
 * se enumeran <b>una a una</b> las columnas que el mundo puede ver, de modo que
 * una columna nueva del agregado no se cuele sola en la respuesta publica.
 *
 * <p>
 * <b>Nada de esto tiene {@code company_id}</b>: {@code price_lists},
 * {@code catalog_prices}, {@code catalog_items}, {@code bundle_components} y
 * {@code catalog_areas} son catalogo global de plataforma. Solo lee.
 *
 * <p>
 * <b>Queda fuera de {@code ADAPTADOR_JPA_CON_RODAJA}</b>, que solo alcanza a
 * los {@code Jpa<Algo>Repository}. Su SQL necesita por tanto una rodaja escrita
 * a mano contra MySQL real —{@code PublicCatalogQueryPortIT}— o no se
 * ejecutaria nunca en el build, que es exactamente como sobrevivio meses el
 * defecto de la incidencia #196.
 *
 * <p>
 * <b>Ni un literal booleano en la proyeccion</b>, y ni un booleano en el
 * destino. Es la leccion de #196 y #472 aplicada a un adaptador que ninguna de
 * las dos reglas de ArchUnit vigila —las dos miran metodos anotados con
 * {@code @Query}, y aqui se usa {@code createNativeQuery}—. MySQL entrega
 * {@code TINYINT} como {@code Byte} y el {@code CASE} devuelve un entero: las
 * columnas de verdad/mentira llegan como {@code Number} a un {@code Object[]} y
 * las convierte {@link #asBoolean(Object)}, en Java, donde el tipo se ve.
 *
 * <p>
 * <b>Toda columna que se anada va al FINAL de su {@code SELECT}.</b> Los bucles
 * leen su {@code Object[]} por posicion, asi que intercalar una columna
 * desplaza todos los indices posteriores y el fallo aparece en ejecucion y no
 * al compilar —{@code asString(columns[7])} sobre un {@code BigDecimal} compila
 * igual—.
 */
@Component
public class JpaPublicCatalogQueryPort implements PublicCatalogQueryPort {

    /**
     * Todo lo que se compra suelto, con su precio de entrada en los DOS ciclos.
     *
     * <p>
     * <b>Los dos {@code JOIN} son {@code LEFT} y el {@code WHERE} exige que al
     * menos uno case.</b> Un articulo tarifado solo en anual es legitimo y tiene
     * que salir —con el mensual nulo—; uno sin ningun precio en la tarifa vigente
     * no se puede comprar, y anunciarlo seria prometer lo que no hay.
     * {@code tier_min = 1} acota al tramo de entrada: la escalera completa es la
     * politica de descuento por volumen y no se publica.
     *
     * <p>
     * <b>El {@code CASE} es el gate, proyectado.</b> Replica el conjunto de
     * aceptacion de
     * {@code JpaPublishedCatalogItemQueryPort.SQL_PUBLISHED_ID_BY_CODE}, que es el
     * paso vinculante: o el articulo es un {@code BUNDLE}, o es un {@code MODULE} o
     * una {@code CAPACITY} que cuelga de algun paquete {@code ACTIVE} publicado
     * <em>o</em> lleva {@code self_service = TRUE}. Junto con "tiene importe en el
     * ciclo pedido", que el consumidor lee de los dos importes, es la condicion
     * exacta que la autocontratacion va a evaluar.
     *
     * <p>
     * <b>La rama de {@code self_service} es la elegibilidad escrita como atributo
     * del articulo.</b> Una fila de {@code bundle_components} significa «incluido
     * en el precio del paquete» y no «vendible aparte»; los cuatro {@code EXTRA_*}
     * son vendibles y no incluidos, asi que la unica forma de abrirles el gate sin
     * anunciarlos como regalados es una columna propia. Los dos SQL cambian a la
     * vez o esta columna miente.
     *
     * <p>
     * <b>El {@code item_type IN ('MODULE', 'CAPACITY')} es lo que sostiene la
     * promesa del contrato publico.</b> El {@code WHERE} exterior deja pasar
     * {@code ONE_TIME}, que es justo la clase que el paso vinculante excluye, y la
     * {@code @Schema} de esta columna promete por escrito que un cargo unico sale
     * como no contratable porque se negocia. Sin este predicado, esa promesa la
     * sostenia la semilla -ningun {@code ONE_TIME} es hoy componente de un paquete-
     * y no el SQL: el dia que alguien metiera uno dentro de un pack, la portada lo
     * anunciaria como autocontratable y el prospecto se estrellaria en el paso 6,
     * ya registrado y con el correo verificado.
     *
     * <p>
     * <b>La rama del {@code BUNDLE} no se puede disparar hoy</b> -el {@code WHERE}
     * exterior no admite ese tipo, los paquetes salen por {@code SQL_PACKS}- y se
     * escribe igualmente para que las tres copias del gate sean el mismo texto. Si
     * manana ese {@code WHERE} se ampliara, su ausencia marcaria todo paquete como
     * no contratable, en silencio y sin que nada fallara.
     *
     * <p>
     * Se proyecta en vez de filtrar por ella para que un {@code ONE_TIME} pueda
     * aparecer con su precio de lista sin ser ofrecido como linea de autoservicio,
     * que es lo que el gate rechazaria.
     *
     * <p>
     * <b>El {@code CASE} sobre {@code trial_eligibility}</b> es lo que impide
     * prometer una prueba que nadie concedio, igual que en
     * {@code JpaPublicPlanQueryPort.SQL_COMPONENTS}: proyecta un entero o nulo,
     * nunca un literal booleano.
     *
     * <p>
     * {@code COALESCE} prefiere el mensual para la tarifa y el tratamiento fiscal:
     * son los mismos en los dos ciclos en cualquier catalogo sano, y si
     * divergieran, el ciclo por defecto de la landing es el mensual. Mismo criterio
     * que {@code SQL_PLANS}.
     */
    private static final String SQL_ITEMS = """
            SELECT ci.code,
                   ci.name,
                   ci.short_description,
                   ci.item_type,
                   ci.structural_minimum,
                   ci.capacity_unit,
                   CASE WHEN ci.trial_eligibility = 'ELIGIBLE'
                        THEN ci.default_trial_days END,
                   pm.unit_amount,
                   pa.unit_amount,
                   pm.included_quantity,
                   pa.included_quantity,
                   COALESCE(pm.setup_amount, pa.setup_amount),
                   COALESCE(pm.tax_rate, pa.tax_rate),
                   COALESCE(pm.tax_treatment, pa.tax_treatment),
                   CASE WHEN ci.item_type = 'BUNDLE'
                             OR (ci.item_type IN ('MODULE', 'CAPACITY')
                                 AND (ci.self_service = TRUE
                                      OR EXISTS (SELECT 1
                                                   FROM bundle_components bc
                                                   JOIN catalog_items b ON b.id = bc.bundle_item_id
                                                  WHERE bc.component_item_id = ci.id
                                                    AND bc.enabled = TRUE
                                                    AND b.enabled = TRUE
                                                    AND b.item_type = 'BUNDLE'
                                                    AND b.status = 'ACTIVE')))
                        THEN 1 ELSE 0 END,
                   ci.area_code,
                   ci.short_label
              FROM catalog_items ci
              LEFT JOIN catalog_prices pm
                     ON pm.catalog_item_id = ci.id
                    AND pm.price_list_id = :priceListId
                    AND pm.billing_cycle = 'MONTHLY'
                    AND pm.tier_min = 1
                    AND pm.enabled = TRUE
              LEFT JOIN catalog_prices pa
                     ON pa.catalog_item_id = ci.id
                    AND pa.price_list_id = :priceListId
                    AND pa.billing_cycle = 'ANNUAL'
                    AND pa.tier_min = 1
                    AND pa.enabled = TRUE
             WHERE ci.item_type IN ('MODULE', 'CAPACITY', 'ONE_TIME')
               AND ci.status = 'ACTIVE'
               AND ci.enabled = TRUE
               AND (pm.id IS NOT NULL OR pa.id IS NOT NULL)
             ORDER BY ci.sort_order, ci.id
            """;

    /**
     * {@code recommended} sale por un {@code CASE … THEN 1 ELSE 0} por lo que dice
     * el javadoc de clase sobre #196 y #472.
     */
    private static final String SQL_PACKS = """
            SELECT b.code,
                   b.name,
                   b.short_description,
                   pm.unit_amount,
                   pa.unit_amount,
                   COALESCE(pm.setup_amount, pa.setup_amount),
                   COALESCE(pm.tax_rate, pa.tax_rate),
                   COALESCE(pm.tax_treatment, pa.tax_treatment),
                   CASE WHEN b.recommended = TRUE THEN 1 ELSE 0 END
              FROM catalog_items b
              LEFT JOIN catalog_prices pm
                     ON pm.catalog_item_id = b.id
                    AND pm.price_list_id = :priceListId
                    AND pm.billing_cycle = 'MONTHLY'
                    AND pm.tier_min = 1
                    AND pm.enabled = TRUE
              LEFT JOIN catalog_prices pa
                     ON pa.catalog_item_id = b.id
                    AND pa.price_list_id = :priceListId
                    AND pa.billing_cycle = 'ANNUAL'
                    AND pa.tier_min = 1
                    AND pa.enabled = TRUE
             WHERE b.item_type = 'BUNDLE'
               AND b.status = 'ACTIVE'
               AND b.enabled = TRUE
               AND (pm.id IS NOT NULL OR pa.id IS NOT NULL)
             ORDER BY b.sort_order, b.id
            """;

    /**
     * La composicion, solo por rotulos.
     *
     * <p>
     * <b>Sin filtro por precio en el componente</b>, y es deliberado: aqui la
     * pregunta no es «se puede comprar esta pieza suelta» sino «viene dentro de
     * este paquete». Una pieza sin tarifar suelta sigue estando dentro del paquete
     * y sigue sin poder comprarse ademas de el, que es lo unico que esta lista
     * tiene que sostener. Filtrarla dejaria fuera de la advertencia justo a la
     * pieza que ningun otro sitio menciona.
     */
    private static final String SQL_PACK_COMPONENTS = """
            SELECT bi.code, ci.code
              FROM bundle_components bc
              JOIN catalog_items bi ON bi.id = bc.bundle_item_id
              JOIN catalog_items ci ON ci.id = bc.component_item_id
             WHERE bc.enabled = TRUE
               AND bi.enabled = TRUE
               AND bi.item_type = 'BUNDLE'
               AND bi.status = 'ACTIVE'
               AND ci.enabled = TRUE
               AND ci.status = 'ACTIVE'
             ORDER BY bi.code, ci.sort_order, ci.id
            """;

    /**
     * Los arcos {@code REQUIRES}, por rotulos.
     *
     * <p>
     * <b>Es el mismo {@code WHERE} que
     * {@code JpaCatalogItemDependencyQueryPort.SQL}, columna por columna</b>, y esa
     * coincidencia es el punto: aquel es el grafo que {@code RequiredItemsClosure}
     * recorre para completar el carrito de verdad. Si este publicara un predicado
     * distinto —anadiendo, por ejemplo, un filtro por precio para que ningun codigo
     * quedara colgando—, la portada anunciaria un grafo y la contratacion aplicaria
     * otro, que es exactamente el desajuste que este endpoint existe para cerrar.
     *
     * <p>
     * <b>Los dos extremos vivos.</b> {@code status = 'ACTIVE'} y {@code enabled} en
     * el articulo <em>y</em> en su requisito, ademas de en la propia dependencia:
     * un arco hacia algo retirado exigiria al prospecto anadir un articulo que ya
     * no se vende y dejaria el carrito imposible de completar.
     *
     * <p>
     * <b>Sin {@code priceListId}</b>: la tabla no tiene columna de tarifa. El orden
     * es el de presentacion del catalogo —{@code sort_order}— para que la lista
     * salga estable entre peticiones y el diff de un contrato no baile.
     */
    private static final String SQL_REQUIREMENTS = """
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

    /**
     * <b>El {@code ORDER BY} es el contrato de presentacion.</b> La respuesta
     * publica no lleva {@code sort_order}: el orden de la lista <em>es</em> el
     * orden en que se pintan, misma convencion que ya sostienen los articulos y los
     * paquetes. El desempate por {@code id} sobra hoy —
     * {@code uq_catalog_areas_sort_order} impide el empate— y se escribe igual para
     * que la lista siga siendo estable si esa unica se retirara.
     */
    private static final String SQL_AREAS = """
            SELECT a.code, a.name
              FROM catalog_areas a
             WHERE a.enabled = TRUE
             ORDER BY a.sort_order, a.id
            """;

    private final EntityManager entityManager;

    public JpaPublicCatalogQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<PublicCatalogItemRowDto> findContractableItems(Long priceListId) {
        if (priceListId == null) {
            return List.of();
        }
        Query query = entityManager.createNativeQuery(SQL_ITEMS).setParameter("priceListId",
                priceListId);
        List<PublicCatalogItemRowDto> articulos = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            articulos.add(new PublicCatalogItemRowDto(asString(columns[0]), asString(columns[1]),
                    asString(columns[2]), asString(columns[3]), asBoolean(columns[4]),
                    asString(columns[5]), asInteger(columns[6]), asAmount(columns[7]),
                    asAmount(columns[8]), asInteger(columns[9]), asInteger(columns[10]),
                    asAmount(columns[11]), asAmount(columns[12]), asTaxTreatment(columns[13]),
                    asBoolean(columns[14]), asString(columns[15]), asString(columns[16])));
        }
        return List.copyOf(articulos);
    }

    @Override
    public List<PublicCatalogPackRowDto> findPacks(Long priceListId) {
        if (priceListId == null) {
            return List.of();
        }
        Query query = entityManager.createNativeQuery(SQL_PACKS).setParameter("priceListId",
                priceListId);
        List<PublicCatalogPackRowDto> paquetes = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            paquetes.add(new PublicCatalogPackRowDto(asString(columns[0]), asString(columns[1]),
                    asString(columns[2]), asAmount(columns[3]), asAmount(columns[4]),
                    asAmount(columns[5]), asAmount(columns[6]), asTaxTreatment(columns[7]),
                    asBoolean(columns[8])));
        }
        return List.copyOf(paquetes);
    }

    @Override
    public List<PublicCatalogPackComponentRowDto> findPackComponents(Long priceListId) {
        if (priceListId == null) {
            return List.of();
        }
        Query query = entityManager.createNativeQuery(SQL_PACK_COMPONENTS);
        List<PublicCatalogPackComponentRowDto> lineas = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            lineas.add(new PublicCatalogPackComponentRowDto(asString(columns[0]),
                    asString(columns[1])));
        }
        return List.copyOf(lineas);
    }

    @Override
    public List<PublicCatalogRequirementRowDto> findRequirements() {
        Query query = entityManager.createNativeQuery(SQL_REQUIREMENTS);
        List<PublicCatalogRequirementRowDto> arcos = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            arcos.add(
                    new PublicCatalogRequirementRowDto(asString(columns[0]), asString(columns[1])));
        }
        return List.copyOf(arcos);
    }

    @Override
    public List<PublicCatalogAreaRowDto> findAreas() {
        Query query = entityManager.createNativeQuery(SQL_AREAS);
        List<PublicCatalogAreaRowDto> areas = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            areas.add(new PublicCatalogAreaRowDto(asString(columns[0]), asString(columns[1])));
        }
        return List.copyOf(areas);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private static BigDecimal asAmount(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof BigDecimal decimal
                ? decimal
                : new BigDecimal(String.valueOf(value));
    }

    /**
     * Una columna de verdad/mentira de una consulta nativa, convertida donde el
     * tipo se ve.
     *
     * <p>
     * MySQL entrega {@code TINYINT} como {@code Byte} y el {@code CASE … THEN 1
     * ELSE 0} como entero; ninguno de los dos es un {@code Boolean} y nadie los
     * convierte solo. Es el defecto de la incidencia #472 —que tumbo el alta de
     * empresa entera— evitado por construccion: aqui no hay proyeccion automatica a
     * la que pedirle el milagro, se lee el {@code Number} y se compara.
     * {@code Boolean} se acepta tambien porque otro dialecto podria entregarlo asi,
     * igual que {@code asLocalDate} acepta las dos formas de una fecha.
     */
    private static boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean flag) {
            return flag;
        }
        return ((Number) value).intValue() != 0;
    }

    private static TaxTreatment asTaxTreatment(Object value) {
        return value == null ? null : TaxTreatment.valueOf(String.valueOf(value));
    }
}
