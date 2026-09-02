package com.vetsoftware.app.entitlement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import com.vetsoftware.app.entitlement.domain.ModuleGrantLine;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * La rodaja que le faltaba a {@link JpaSubscriptionQueryPort}, y la prueba de
 * regresión de la incidencia #472.
 *
 * <p>
 * <strong>Con un mock del repositorio esto pasa en verde y no prueba
 * nada.</strong> El defecto de #472 no estaba en la consulta ni en la
 * traducción: estaba en el salto entre el {@code ResultSet} de MySQL y la
 * interfaz de proyección de Spring Data, que solo existe cuando hay un MySQL de
 * verdad devolviendo un {@code TINYINT} de verdad. Un doble devuelve el
 * {@code ContractModuleLineView} que el propio test fabrica, con los tipos que
 * al test le apetezcan, y el {@code ProjectingMethodInterceptor} —que es quien
 * reventaba— no llega ni a instanciarse.
 *
 * <p>
 * Y el fallo tampoco aparece con la tabla vacía: la excepción salta al leer la
 * <b>primera fila</b>. Por eso todos estos casos exigen que {@link SchemaSeed}
 * haya sembrado al menos una línea de contrato con su artículo de catálogo y su
 * submódulo — que es exactamente la condición que el catálogo comercial vacío
 * escondió durante meses en local, en e2e y en dev.
 */
@Import(JpaSubscriptionQueryPort.class)
@DisplayName("JpaSubscriptionQueryPort — el contrato proyectado contra MySQL real")
class ContractSnapshotPersistenceIT extends AbstractDataJpaTest {

    /** Cualquier día dentro de la vigencia del contrato que siembra SchemaSeed. */
    private static final LocalDate HOY = LocalDate.of(2026, 6, 1);

    @Autowired
    private JpaSubscriptionQueryPort queryPort;
    @PersistenceContext
    private EntityManager entityManager;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
    }

    @Nested
    @DisplayName("Proyección nativa de columnas TINYINT (#472)")
    class ProyeccionDeBooleanos {

        @Test
        @DisplayName("lee la línea de módulo sin reventar al proyectar el TINYINT")
        void lee_la_linea_de_modulo_sin_reventar_al_proyectar_el_tinyint() {
            Optional<ContractSnapshot> contrato = queryPort
                    .findCurrentContractByCompanyId(SchemaSeed.COMPANY_ID, HOY);

            assertThat(contrato).isPresent();
            assertThat(contrato.get().moduleLines()).singleElement().satisfies(linea -> {
                assertThat(linea.subscriptionItemId()).isEqualTo(SchemaSeed.SUBSCRIPTION_ITEM_ID);
                assertThat(linea.subModule().id()).isEqualTo(SchemaSeed.SUB_MODULE_ID);
                // Las dos columnas TINYINT del defecto: sub_modules.read_only_capable
                // y catalog_items.structural_minimum, sembradas a 1.
                assertThat(linea.readOnlyCapable()).isTrue();
                assertThat(linea.core()).isTrue();
            });
        }

        @Test
        @DisplayName("un TINYINT en cero se traduce a falso, no a nulo ni a excepción")
        void un_tinyint_en_cero_se_traduce_a_falso() {
            entityManager
                    .createNativeQuery(
                            "UPDATE sub_modules SET read_only_capable = 0 WHERE id = :id")
                    .setParameter("id", SchemaSeed.SUB_MODULE_ID).executeUpdate();
            entityManager
                    .createNativeQuery(
                            "UPDATE catalog_items SET structural_minimum = 0 WHERE id = :id")
                    .setParameter("id", nucleo).executeUpdate();
            entityManager.flush();
            entityManager.clear();

            ContractSnapshot contrato = queryPort
                    .findCurrentContractByCompanyId(SchemaSeed.COMPANY_ID, HOY).orElseThrow();

            assertThat(contrato.moduleLines()).singleElement().satisfies(linea -> {
                assertThat(linea.readOnlyCapable()).isFalse();
                assertThat(linea.core()).isFalse();
            });
        }
    }

    @Nested
    @DisplayName("Resolución del contrato")
    class ResolucionDelContrato {

        @Test
        @DisplayName("devuelve la cabecera vigente con su estado y su vigencia")
        void devuelve_la_cabecera_vigente() {
            ContractSnapshot contrato = queryPort
                    .findCurrentContractByCompanyId(SchemaSeed.COMPANY_ID, HOY).orElseThrow();

            assertThat(contrato.subscription().id()).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
            assertThat(contrato.subscription().status().name()).isEqualTo("ACTIVE");
            assertThat(contrato.moduleLines()).extracting(ModuleGrantLine::core)
                    .containsExactly(true);
        }

        @Test
        @DisplayName("no cruza empresas: cada una ve solo las líneas de su contrato")
        void no_cruza_empresas() {
            ContractSnapshot ajeno = queryPort
                    .findCurrentContractByCompanyId(SchemaSeed.OTRA_COMPANY_ID, HOY).orElseThrow();

            assertThat(ajeno.subscription().id()).isEqualTo(SchemaSeed.OTRA_SUBSCRIPTION_ID);
            assertThat(ajeno.moduleLines()).extracting(ModuleGrantLine::subscriptionItemId)
                    .containsExactly(SchemaSeed.OTRO_SUBSCRIPTION_ITEM_ID);
        }

        @Test
        @DisplayName("una empresa sin contrato devuelve vacío en las dos consultas")
        void una_empresa_sin_contrato_devuelve_vacio() {
            Long inexistente = -1L;

            assertThat(queryPort.findCurrentContractByCompanyId(inexistente, HOY)).isEmpty();
            assertThat(queryPort.findLatestContractByCompanyId(inexistente)).isEmpty();
        }
    }
}
