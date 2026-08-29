package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.application.dto.PublicCatalogItemRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicCatalogPackComponentRowDto;
import com.vetsoftware.app.pricelist.application.dto.PublicPlanRowDto;
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
 * {@code catalog_prices}, {@code catalog_items} y {@code bundle_components} son
 * catalogo global de plataforma. Solo lee.
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
 * dos columnas llegan como {@code Number} a un {@code Object[]} y las convierte
 * {@link #asBoolean(Object)}, en Java, donde el tipo se ve.
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
     * <b>La ultima columna es el gate, proyectado.</b> Ese {@code EXISTS} es
     * <em>literalmente</em> la rama del componente de
     * {@code JpaPublishedCatalogItemQueryPort.SQL_PUBLISHED_ID_BY_CODE}: el
     * articulo cuelga de algun paquete {@code ACTIVE} publicado. Junto con «tiene
     * importe en el ciclo pedido» —que el consumidor lee de los dos importes— es la
     * condicion exacta que la autocontratacion va a evaluar. Se publica en vez de
     * filtrar por ella para que un {@code ONE_TIME} pueda aparecer con su precio de
     * lista sin ser ofrecido como linea de autoservicio, que es lo que el gate
     * rechazaria.
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
                   ci.is_core,
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
                   CASE WHEN EXISTS (SELECT 1
                                       FROM bundle_components bc
                                       JOIN catalog_items b ON b.id = bc.bundle_item_id
                                      WHERE bc.component_item_id = ci.id
                                        AND bc.enabled = TRUE
                                        AND b.enabled = TRUE
                                        AND b.item_type = 'BUNDLE'
                                        AND b.status = 'ACTIVE')
                        THEN 1 ELSE 0 END
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
     * Los paquetes. Es el mismo {@code SELECT} que
     * {@code JpaPublicPlanQueryPort.SQL_PLANS} y proyecta al mismo record, porque
     * es la misma pregunta: un paquete vendible con precio de entrada.
     */
    private static final String SQL_PACKS = """
            SELECT b.code,
                   b.name,
                   b.short_description,
                   pm.unit_amount,
                   pa.unit_amount,
                   COALESCE(pm.setup_amount, pa.setup_amount),
                   COALESCE(pm.tax_rate, pa.tax_rate),
                   COALESCE(pm.tax_treatment, pa.tax_treatment)
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
                    asBoolean(columns[14])));
        }
        return List.copyOf(articulos);
    }

    @Override
    public List<PublicPlanRowDto> findPacks(Long priceListId) {
        if (priceListId == null) {
            return List.of();
        }
        Query query = entityManager.createNativeQuery(SQL_PACKS).setParameter("priceListId",
                priceListId);
        List<PublicPlanRowDto> paquetes = new ArrayList<>();
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            paquetes.add(new PublicPlanRowDto(asString(columns[0]), asString(columns[1]),
                    asString(columns[2]), asAmount(columns[3]), asAmount(columns[4]),
                    asAmount(columns[5]), asAmount(columns[6]), asTaxTreatment(columns[7])));
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
