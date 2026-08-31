package com.vetsoftware.app.catalogitemaihint.infrastructure.persistence;

import com.vetsoftware.app.catalogitemaihint.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.catalogitemaihint.domain.CatalogItemRef;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta feature que conoce {@code catalog_items}.
 *
 * <p>
 * &#9888; <strong>Se llama {@code JpaAiHintCatalogItemQueryPort} y no
 * {@code JpaCatalogItemQueryPort} a proposito.</strong> Ese nombre ya lo ocupa
 * el adaptador homonimo de {@code pricelist}, y el generador de nombres de bean
 * de Spring usa el nombre simple de la clase: dos
 * {@code JpaCatalogItemQueryPort} en el classpath dan un
 * {@code ConflictingBeanDefinitionException} y <b>ningun contexto de la
 * aplicacion arranca</b> —ni el de produccion ni el de un solo
 * {@code @SpringBootTest}—, con un fallo que no senala a ninguna de las dos
 * clases. El puerto si conserva el nombre canonico: las interfaces no producen
 * bean.
 *
 * <p>
 * &#9940; <strong>SQL nativo con el filtro escrito, y no el
 * {@code CatalogItemJpaRepository} del slice vecino. Esto es una correccion, no
 * una preferencia.</strong> La version anterior delegaba en
 * {@code findById}/{@code findAllById} de Spring Data y confiaba el filtrado al
 * {@code @SQLRestriction("enabled = true")} que declara
 * {@code CatalogItemJpaEntity}.
 *
 * <p>
 * <b>El agujero: ningun camino miraba {@code status}</b>, que es donde vive el
 * estado comercial. Un articulo en {@code DRAFT} todavia se esta redactando y
 * uno en {@code DEPRECATED} se retiro de la venta, pero los dos siguen con
 * {@code enabled = TRUE}, asi que la guarda de {@link #findById} los daba por
 * buenos y dejaba publicarles pista. El resultado es el peor posible para esta
 * tabla —la pista queda <em>vigente</em> y el prompt le ensena al modelo el
 * codigo de un modulo que el motor de cotizacion rechazara despues— y lo
 * descubre el prospecto, no el build.
 * {@code JpaCatalogQueryPorts.JpaCatalogItemQueryPort} de {@code quote} ya
 * filtraba por {@code status = 'ACTIVE'} por este mismo razonamiento; esta
 * clase es ahora su gemela.
 *
 * <p>
 * &#9888; <strong>Lo que el {@code @SQLRestriction} SI cubria, medido y no
 * supuesto.</strong> Quedaba la duda de si esa restriccion alcanza la carga por
 * clave primaria —el {@code @Where} historico de Hibernate no lo hacia, y de
 * ahi que sea una pregunta discutible—. Con el Hibernate de Spring Boot 4.1
 * <b>si la alcanza</b>: el SQL de {@code findById} sale con
 * {@code where id=? and (enabled = true)}, igual que el de {@code findAllById}.
 * O sea que el comentario anterior no mentia sobre {@code enabled} y los dos
 * metodos filtraban lo mismo; el defecto era exclusivamente {@code status}. La
 * evidencia esta fijada contra MySQL real en
 * {@code AiHintCatalogItemQueryPortIT.SqlRestriction}.
 *
 * <p>
 * <strong>Aun asi el filtro se escribe aqui y no se hereda</strong>: heredarlo
 * ata esta guarda —que decide si se puede publicar— a una anotacion del slice
 * vecino que puede cambiar sin que nadie lo note en esta feature, y que nunca
 * ha cubierto la mitad que importa.
 *
 * <p>
 * <strong>Los dos metodos SI filtran distinto ahora, y es deliberado.</strong>
 * {@link #findById} es una guarda de escritura y exige {@code ACTIVE};
 * {@link #findAllByIds} solo pinta el nombre de filas que ya existen y se queda
 * en {@code enabled}. Es la misma division que hacen {@code quote}
 * —{@code findActiveById}, documento con valor legal— y {@code pricelist}
 * —{@code findAllByIds} sin {@code status}, para no dejar la tarifa historica
 * ilegible—.
 *
 * <p>
 * Solo LEE, y {@code catalog_items} no tiene {@code company_id} —es catalogo
 * global de plataforma—, asi que no hay empresa que acotar.
 */
@Component
public class JpaAiHintCatalogItemQueryPort implements CatalogItemQueryPort {

    private final EntityManager entityManager;

    public JpaAiHintCatalogItemQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * El articulo, solo si esta {@code ACTIVE} y habilitado.
     *
     * <p>
     * &#9940; <strong>El filtro por {@code status} no es cosmetico y es lo que
     * separa esta consulta de la de al lado.</strong> Publicar una pista es
     * ensenarle al modelo que puede proponer ese modulo: hacerlo sobre uno en
     * {@code DRAFT} —que todavia se esta redactando— o {@code DEPRECATED} —que se
     * retiro de la venta— produce recomendaciones de algo que la plataforma no
     * vende, y la unica senal que recibe el prospecto es que su cotizacion falla
     * mas tarde.
     */
    @Override
    public Optional<CatalogItemRef> findById(Long catalogItemId) {
        if (catalogItemId == null) {
            return Optional.empty();
        }
        Query query = entityManager.createNativeQuery("""
                SELECT id, code, name
                  FROM catalog_items
                 WHERE id = :id
                   AND status = 'ACTIVE'
                   AND enabled = TRUE
                """).setParameter("id", catalogItemId);
        List<?> filas = query.setMaxResults(1).getResultList();
        return filas.isEmpty() ? Optional.empty() : Optional.of(toRef((Object[]) filas.get(0)));
    }

    /**
     * Una consulta por pagina, no una por fila: el {@code IN (…)} es lo que evita
     * el N+1 al pintar un listado de veinte pistas.
     *
     * <p>
     * <strong>No filtra por {@code status}, a diferencia de {@link #findById}, y
     * esa diferencia es la decision.</strong> Aqui no se esta autorizando nada:
     * solo se pone el codigo y el nombre a filas del historial que ya existen. La
     * pista de un articulo retirado sigue siendo una fila legitima —de hecho es la
     * que mas interesa leer cuando se revisa por que el modelo proponia algo— y
     * esconder su nombre dejaria el historial ilegible sin impedir ninguna
     * escritura. Es el mismo criterio que {@code pricelist} dejo escrito para la
     * tarifa historica. Los articulos que no aparezcan quedan fuera del mapa y el
     * DTO los sirve con {@code catalogItemCode} y {@code catalogItemName} nulos.
     */
    @Override
    public Map<Long, CatalogItemRef> findAllByIds(Collection<Long> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = catalogItemIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Query query = entityManager.createNativeQuery("""
                SELECT id, code, name
                  FROM catalog_items
                 WHERE id IN (:ids)
                   AND enabled = TRUE
                """).setParameter("ids", ids);
        Map<Long, CatalogItemRef> resueltos = new LinkedHashMap<>();
        for (Object fila : query.getResultList()) {
            CatalogItemRef ref = toRef((Object[]) fila);
            resueltos.put(ref.id(), ref);
        }
        return Map.copyOf(resueltos);
    }

    private static CatalogItemRef toRef(Object[] columnas) {
        return new CatalogItemRef(((Number) columnas[0]).longValue(), String.valueOf(columnas[1]),
                String.valueOf(columnas[2]));
    }
}
