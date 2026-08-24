package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.submodule.application.port.out.CatalogItemChildrenQueryPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/**
 * Cuenta los {@code catalog_item_sub_modules} vivos que cuelgan de un
 * submodulo.
 *
 * <p>
 * <b>Por que la consulta va en SQL nativo y no por el repositorio Spring Data
 * de {@code catalogitem}.</b> El slice {@code catalogitem} se esta escribiendo
 * en paralelo a este cambio, y un metodo derivado como
 * {@code existsBySubModule_Id} solo existe si <em>alguien lo declara</em> en
 * esa interfaz: acertar el nombre de un metodo que todavia no esta escrito es
 * apostar el build entero de este slice a una coincidencia. El nombre de la
 * <em>tabla</em>, en cambio, es normativo y esta fijado en la especificacion
 * ({@code docs/db/suscripciones-tablas.md}), asi que esta consulta no depende
 * de nada en vuelo. <b>Cuando ese slice aterrice, esto debe pasar a
 * {@code CatalogItemSubModuleJpaRepository}</b> — es el patron de la casa y lo
 * que las reglas de arquitectura saben leer.
 *
 * <p>
 * Se proyecta un {@code COUNT(*)} y la comparacion con cero se hace en Java:
 * {@code PROYECCION_SIN_LITERAL_BOOLEANO} (#196) prohibe devolver un literal
 * booleano desde el {@code SELECT}.
 */
@Component
public class JpaCatalogItemChildrenQueryPort implements CatalogItemChildrenQueryPort {

    private static final String COUNT_VIVOS = """
            SELECT COUNT(*)
            FROM catalog_item_sub_modules
            WHERE sub_module_id = :subModuleId
              AND enabled = true
            """;

    private final EntityManager entityManager;

    public JpaCatalogItemChildrenQueryPort(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsActiveBySubModuleId(Long subModuleId) {
        Number total = (Number) entityManager.createNativeQuery(COUNT_VIVOS)
                .setParameter("subModuleId", subModuleId).getSingleResult();
        return total.longValue() > 0;
    }
}
