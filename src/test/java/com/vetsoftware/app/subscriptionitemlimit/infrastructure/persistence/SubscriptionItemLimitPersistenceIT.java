package com.vetsoftware.app.subscriptionitemlimit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionitemlimit.domain.LimitEnforcement;
import com.vetsoftware.app.subscriptionitemlimit.domain.LimitMode;
import com.vetsoftware.app.subscriptionitemlimit.domain.MeasureKind;
import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionItemLimitRepository — el techo congelado contra MySQL real")
class SubscriptionItemLimitPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDateTime FIRMA = LocalDateTime.of(2026, 1, 15, 8, 0);

    @Autowired
    private JpaSubscriptionItemLimitRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Resuelto, no sembrado: los ocho ejes llegan poblados por el changeset 313.
     */
    private Long ejeAnimal;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
        entityManager.flush();
        ejeAnimal = SchemaSeed.limitDimensionId(entityManager, "ANIMAL");
    }

    private SubscriptionItemLimit congelado(Long companyId, Long subscriptionItemId, int cantidad) {
        return SubscriptionItemLimit.freeze(companyId, subscriptionItemId, ejeAnimal,
                MeasureKind.CUMULATIVE, LimitMode.LIMITED, cantidad, null, LimitEnforcement.BLOCK,
                null, 80, FIRMA);
    }

    @Test
    @DisplayName("congela el techo en la línea del contrato y lo vuelve a leer acotado por"
            + " empresa")
    void congela_el_techo_en_la_linea_y_lo_lee_acotado_por_empresa() {
        repository.save(congelado(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, 100));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByCompanyIdAndSubscriptionItemIdAndLimitDimensionId(
                SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, ejeAnimal)).get()
                .satisfies(leido -> assertThat(leido.getLimitQuantity()).isEqualTo(100));
    }

    @Test
    @DisplayName("el techo de una clínica no se ve desde otra")
    void el_techo_de_una_clinica_no_se_ve_desde_otra() {
        repository.save(congelado(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, 100));
        repository.save(
                congelado(SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRO_SUBSCRIPTION_ITEM_ID, 300));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).singleElement()
                .satisfies(limite -> assertThat(limite.getLimitQuantity()).isEqualTo(100));
        assertThat(repository.findAllByCompanyId(SchemaSeed.OTRA_COMPANY_ID)).singleElement()
                .satisfies(limite -> assertThat(limite.getLimitQuantity()).isEqualTo(300));
    }

    @Test
    @DisplayName("R-LIMIT-36 · la consulta de propagación alcanza las líneas vivas de las dos"
            + " empresas, que es lo que hace que una mejora llegue a todos los contratos")
    void la_consulta_de_propagacion_alcanza_las_lineas_vivas_de_las_dos_empresas() {
        repository.save(congelado(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, 100));
        repository.save(
                congelado(SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRO_SUBSCRIPTION_ITEM_ID, 100));
        entityManager.flush();
        entityManager.clear();

        List<SubscriptionItemLimit> vivos = repository
                .findAllLiveByCatalogItemIdAndLimitDimensionId(nucleo, ejeAnimal);

        assertThat(vivos).hasSize(2).extracting(SubscriptionItemLimit::getCompanyId)
                .containsExactlyInAnyOrder(SchemaSeed.COMPANY_ID, SchemaSeed.OTRA_COMPANY_ID);
    }

    @Test
    @DisplayName("una línea cerrada queda fuera de la propagación: la mejora no reescribe el techo"
            + " de un contrato que terminó")
    void una_linea_cerrada_queda_fuera_de_la_propagacion() {
        repository.save(congelado(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, 100));
        entityManager.flush();
        entityManager.createNativeQuery("""
                UPDATE subscription_items
                SET effective_to = '2026-06-30', version = version + 1
                WHERE id = :itemId AND company_id = :companyId
                """).setParameter("itemId", SchemaSeed.SUBSCRIPTION_ITEM_ID)
                .setParameter("companyId", SchemaSeed.COMPANY_ID).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllLiveByCatalogItemIdAndLimitDimensionId(nucleo, ejeAnimal))
                .isEmpty();
    }

    @Test
    @DisplayName("guardar en bloque una mejora deja los techos nuevos en la base")
    void guardar_en_bloque_una_mejora_deja_los_techos_nuevos() {
        SubscriptionItemLimit uno = repository
                .save(congelado(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, 100));
        entityManager.flush();

        uno.improveFrom(LimitMode.LIMITED, 200);
        repository.saveAll(List.of(uno));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID)).singleElement()
                .satisfies(limite -> assertThat(limite.getLimitQuantity()).isEqualTo(200));
    }

    @Test
    @DisplayName("guardar una lista vacía no toca la base")
    void guardar_una_lista_vacia_no_toca_la_base() {
        assertThat(repository.saveAll(List.of())).isEmpty();
    }
}
