package com.vetsoftware.app.platformbillingconfig.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPlatformBillingConfigRepository — singleton de facturación contra MySQL real")
class PlatformBillingConfigPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaPlatformBillingConfigRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("actualiza la única fila e hidrata la lista de precios referenciada")
    void actualiza_el_singleton_e_hidrata_la_tarifa() {
        PlatformBillingConfig config = repository.find().orElseThrow();
        config.update(new PriceListRef(SchemaSeed.PRICE_LIST_ID, "LISTA-TEST", "Lista de prueba"),
                7, 21, 15, 3, "SIIGO");

        repository.save(config);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.find()).get().satisfies(leida -> {
            assertThat(leida.getDefaultPriceList().id()).isEqualTo(SchemaSeed.PRICE_LIST_ID);
            assertThat(leida.getDefaultPriceList().code()).isEqualTo("LISTA-TEST");
            assertThat(leida.getDefaultGraceDays()).isEqualTo(7);
            assertThat(leida.getInvoiceDayOfMonth()).isEqualTo(15);
            assertThat(leida.getExternalBillingProvider()).isEqualTo("SIIGO");
        });
    }
}
