package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemCompositionPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Congela la composicion del articulo copiandola del catalogo VIVO en el
 * instante de firmar (D-76).
 *
 * <p>
 * <b>El {@code INSERT ... SELECT} es deliberado</b>: la foto tiene que salir
 * del mismo estado del catalogo que ve esta transaccion, y traerla a Java para
 * volver a escribirla abriria una ventana en la que un cambio de catalogo
 * concurrente dejaria una foto que nunca existio.
 *
 * <p>
 * Las dos sentencias son las dos ramas de la consulta que el recalculo dejo de
 * hacer: los submodulos que el articulo ata directamente, y —solo para los
 * paquetes— los de sus componentes. Las dos son idempotentes: un paquete puede
 * llegar al mismo submodulo por dos componentes distintos y la foto es un
 * conjunto, y una segunda pasada sobre la misma linea no puede duplicar nada.
 *
 * <p>
 * ⛔ <b>{@code ON DUPLICATE KEY UPDATE} y NO {@code INSERT IGNORE}, y la
 * diferencia es toda la garantia de tenencia.</b> Las dos clausulas dan la
 * misma idempotencia frente a {@code uq_subscription_item_sub_modules}, pero
 * {@code IGNORE} degrada a <em>warning</em> <b>cualquier</b> error de la
 * sentencia —incluida la violacion de clave foranea—, y aqui la clave foranea
 * <b>es</b> el control: el {@code company_id} viaja como parametro y el
 * {@code SELECT} de origen solo lee el catalogo global, asi que lo unico que
 * rechaza una llamada con la empresa equivocada es
 * {@code fk_subscription_item_sub_modules_item}
 * ({@code (company_id, subscription_item_id) → subscription_items(company_id,
 * id)}). Con {@code IGNORE} esa violacion no lanzaba, no se registraba, y la
 * operacion devolvia <b>cero</b> — indistinguible del cero legitimo de una
 * linea de capacidad sin composicion, y con los dos llamantes descartando el
 * valor de retorno. Es decir: una firma cruzada entre clinicas quedaba
 * silenciosamente sin foto, o sea sin permisos, y nadie se enteraba.
 * {@code ON DUPLICATE KEY UPDATE} solo absorbe el duplicado de ese unico
 * indice; la foranea vuelve a lanzar.
 *
 * <p>
 * <b>El {@code SET} es un no-op deliberado y va CUALIFICADO con el nombre de la
 * tabla destino.</b> La fila que ya existe es la foto buena y no se reescribe
 * —ni su {@code created_date} ni su {@code enabled}—. La cualificacion no es
 * estilo: en un {@code INSERT ... SELECT}, un nombre de columna suelto en el
 * {@code ON DUPLICATE KEY UPDATE} compite con las columnas de las tablas del
 * {@code SELECT} y MySQL lo rechaza por ambiguo.
 *
 * <p>
 * &#9888; <b>Y por eso el retorno NO puede salir de
 * {@code executeUpdate()}.</b> Cambiar la clausula cambia lo que ese entero
 * cuenta: con {@code CLIENT_FOUND_ROWS} —el modo con el que Connector/J se
 * conecta por defecto— un duplicado absorbido suma <b>1</b> igual que un alta.
 * El numero que el puerto promete lo calcula
 * {@link #cuantosCongelados(Long, Long)}, que mide la foto y no la sentencia.
 *
 * <p>
 * <b>Escribe, y la escritura va ACOTADA POR EMPRESA</b>
 * ({@code MUTACIONES_SQL_ACOTADAS_POR_EMPRESA}): el {@code company_id} viaja
 * como parametro y no se deduce de la linea, para que una linea de otra clinica
 * no pueda arrastrar aqui su composicion.
 */
@Component
public class JpaSubscriptionItemCompositionPort implements SubscriptionItemCompositionPort {

    private final EntityManager entityManager;

    public JpaSubscriptionItemCompositionPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public int freeze(Long companyId, Long subscriptionItemId, Long catalogItemId) {
        if (companyId == null || subscriptionItemId == null || catalogItemId == null)
            throw new IllegalArgumentException(
                    "companyId, subscriptionItemId and catalogItemId are required");
        int antes = cuantosCongelados(companyId, subscriptionItemId);
        entityManager.createNativeQuery("""
                INSERT INTO subscription_item_sub_modules
                            (company_id, subscription_item_id, sub_module_id, created_date)
                SELECT :companyId, :itemId, cism.sub_module_id, CURRENT_TIMESTAMP(6)
                  FROM catalog_item_sub_modules cism
                  JOIN sub_modules sm ON sm.id = cism.sub_module_id AND sm.enabled = TRUE
                 WHERE cism.catalog_item_id = :catalogItemId
                   AND cism.enabled = TRUE
                    ON DUPLICATE KEY UPDATE
                       subscription_item_sub_modules.company_id
                           = subscription_item_sub_modules.company_id
                """).setParameter("companyId", companyId).setParameter("itemId", subscriptionItemId)
                .setParameter("catalogItemId", catalogItemId).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO subscription_item_sub_modules
                            (company_id, subscription_item_id, sub_module_id, created_date)
                SELECT :companyId, :itemId, cism.sub_module_id, CURRENT_TIMESTAMP(6)
                  FROM bundle_components bc
                  JOIN catalog_item_sub_modules cism
                       ON cism.catalog_item_id = bc.component_item_id AND cism.enabled = TRUE
                  JOIN sub_modules sm ON sm.id = cism.sub_module_id AND sm.enabled = TRUE
                 WHERE bc.bundle_item_id = :catalogItemId
                   AND bc.enabled = TRUE
                    ON DUPLICATE KEY UPDATE
                       subscription_item_sub_modules.company_id
                           = subscription_item_sub_modules.company_id
                """).setParameter("companyId", companyId).setParameter("itemId", subscriptionItemId)
                .setParameter("catalogItemId", catalogItemId).executeUpdate();
        return cuantosCongelados(companyId, subscriptionItemId) - antes;
    }

    /**
     * &#9940; <b>Cuantos submodulos hay congelados de verdad, que NO es lo que
     * devuelve {@code executeUpdate()}.</b>
     *
     * <p>
     * Connector/J se conecta con {@code CLIENT_FOUND_ROWS} —{@code useAffectedRows}
     * vale {@code false} por defecto—, asi que en un
     * {@code ON DUPLICATE KEY UPDATE} una fila que ya existia cuenta <b>1</b>
     * aunque no se escriba nada en ella. Sumar los dos {@code executeUpdate()}
     * daria por tanto <b>5</b> donde la foto tiene 4 —el submodulo al que el
     * paquete llega por dos componentes distintos se contaria dos veces— y una
     * segunda pasada devolveria 4 en vez de 0, que es exactamente lo contrario de
     * lo que significa «idempotente». Medido contra MySQL real en
     * {@code SubscriptionItemCompositionPortIT}.
     *
     * <p>
     * Contar la foto antes y despues es la unica forma de que el numero signifique
     * lo que el puerto promete —<i>cuantos submodulos quedaron congelados</i>— con
     * independencia de la clausula con la que se inserte. Son dos {@code COUNT}
     * resueltos por {@code uq_subscription_item_sub_modules}, dentro de la
     * transaccion que ya esta abierta.
     */
    private int cuantosCongelados(Long companyId, Long subscriptionItemId) {
        Object total = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                  FROM subscription_item_sub_modules
                 WHERE company_id = :companyId
                   AND subscription_item_id = :itemId
                """).setParameter("companyId", companyId).setParameter("itemId", subscriptionItemId)
                .getSingleResult();
        return ((Number) total).intValue();
    }

    @Override
    public List<Long> findFrozenSubModuleIds(Long companyId, Long subscriptionItemId) {
        Query query = entityManager.createNativeQuery("""
                SELECT sub_module_id
                  FROM subscription_item_sub_modules
                 WHERE company_id = :companyId
                   AND subscription_item_id = :itemId
                   AND enabled = TRUE
                 ORDER BY sub_module_id
                """).setParameter("companyId", companyId).setParameter("itemId",
                subscriptionItemId);
        return query.getResultList().stream().map(value -> ((Number) value).longValue()).toList();
    }
}
