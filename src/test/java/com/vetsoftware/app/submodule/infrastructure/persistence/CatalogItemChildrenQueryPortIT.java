package com.vetsoftware.app.submodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

/**
 * La guardia que impide borrar un submodulo que el catalogo todavia vende.
 *
 * <p>
 * <b>Nadie ejecutaba su SQL.</b> {@code JpaCatalogItemChildrenQueryPort} lee
 * {@code catalog_item_sub_modules} con una consulta nativa —el propio javadoc
 * explica por que— y, al no ser un {@code Jpa<Algo>Repository}, queda fuera de
 * {@code ADAPTADOR_JPA_CON_RODAJA}. Ningun test del repositorio lo nombraba.
 *
 * <p>
 * <b>Lo que se juega.</b> Si la consulta deja de ver los enlaces vivos, el
 * submodulo se borra y los articulos de catalogo que lo vendian quedan
 * apuntando a nada: las clinicas que compraron ese modulo dejan de tenerlo al
 * siguiente recalculo de permisos. Y si los ve de mas, un submodulo retirado no
 * se puede dar de baja nunca.
 *
 * <p>
 * <b>El adaptador se instancia a mano</b>, como {@code PublicPlanQueryPortIT}:
 * solo necesita el {@code EntityManager} y anadirlo al {@code @Import}
 * cambiaria la clave del {@code MergedContextConfiguration} y costaria un
 * arranque de contexto entero.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCatalogItemChildrenQueryPort — la guardia de borrado del submodulo")
class CatalogItemChildrenQueryPortIT extends AbstractDataJpaTest {

    /** Un submodulo sin ningun articulo que lo venda. */
    private static final long SUB_MODULE_HUERFANO = 9200L;

    @PersistenceContext
    private EntityManager entityManager;

    private JpaCatalogItemChildrenQueryPort port;

    @BeforeEach
    void montarElPuerto() {
        SchemaSeed.seed(entityManager);
        port = new JpaCatalogItemChildrenQueryPort(entityManager);

        entityManager.createNativeQuery("""
                INSERT INTO sub_modules (id, name, code, module_id, created_date, enabled, version,
                                         is_sellable, read_only_capable)
                VALUES (:id, 'Submodulo sin vender', 'TEST_SUB_MODULE_HUERFANO', :modulo,
                        NOW(6), true, 0, true, true)
                """).setParameter("id", SUB_MODULE_HUERFANO)
                .setParameter("modulo", SchemaSeed.MODULE_ID).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * {@link SchemaSeed} enlaza el articulo {@code CORE} con su submodulo de
     * prueba, asi que este es el caso vivo: existe un enlace habilitado y la
     * guardia lo ve.
     */
    @Test
    @DisplayName("un submodulo que un articulo del catalogo vende no se puede borrar")
    void un_submodulo_que_el_catalogo_vende_no_se_puede_borrar() {
        assertThat(port.existsActiveBySubModuleId(SchemaSeed.SUB_MODULE_ID)).isTrue();
    }

    /**
     * <b>El contraste que impide la prueba vacia.</b> Sin un submodulo que de
     * verdad no tenga enlaces, «devuelve falso» seria cierto por cualquier motivo
     * —una consulta rota, un parametro que no liga, un {@code COUNT} sobre la tabla
     * equivocada— y este archivo entero se cumpliria sin comprobar nada.
     */
    @Test
    @DisplayName("un submodulo que nadie vende si se puede borrar")
    void un_submodulo_que_nadie_vende_si_se_puede_borrar() {
        assertThat(port.existsActiveBySubModuleId(SUB_MODULE_HUERFANO)).isFalse();
    }

    /**
     * <b>El {@code enabled = true} del {@code WHERE} es la mitad de la guardia.</b>
     * Un enlace dado de baja logica sigue en la tabla; contarlo dejaria el
     * submodulo bloqueado para siempre, porque nadie puede borrar una fila que ya
     * se retiro. Se apaga el unico enlace que hay y se exige que la respuesta
     * cambie: si alguien quita esa condicion, este caso se pone rojo.
     */
    @Test
    @DisplayName("un enlace dado de baja deja de contar y libera el submodulo")
    void un_enlace_dado_de_baja_deja_de_contar() {
        assertThat(port.existsActiveBySubModuleId(SchemaSeed.SUB_MODULE_ID)).isTrue();

        entityManager.createNativeQuery("""
                UPDATE catalog_item_sub_modules SET enabled = false WHERE sub_module_id = :id
                """).setParameter("id", SchemaSeed.SUB_MODULE_ID).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertThat(port.existsActiveBySubModuleId(SchemaSeed.SUB_MODULE_ID)).isFalse();
    }

    /**
     * Un submodulo que no existe no tiene hijos. Es la respuesta que evita que la
     * guardia reviente cuando se le pregunta por una fila ya borrada.
     */
    @Test
    @DisplayName("un submodulo inexistente no bloquea nada")
    void un_submodulo_inexistente_no_bloquea_nada() {
        assertThat(port.existsActiveBySubModuleId(999_999L)).isFalse();
    }
}
