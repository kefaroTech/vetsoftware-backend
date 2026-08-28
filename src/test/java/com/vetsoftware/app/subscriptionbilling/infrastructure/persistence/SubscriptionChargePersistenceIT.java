package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.RecurringChargeKey;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionChargeRepository — devengos contra MySQL real")
class SubscriptionChargePersistenceIT extends AbstractDataJpaTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-02-01T10:00:00Z"),
            ZoneOffset.UTC);
    private static final ServicePeriod PERIOD = new ServicePeriod(LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 28));

    @Autowired
    private JpaSubscriptionChargeRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Test
    @DisplayName("guarda el devengo pendiente con importe fiscal y periodo exactos")
    void guarda_devengo_pendiente_con_periodo_exacto() {
        SubscriptionCharge saved = repository.save(SubscriptionCharge.create(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, ChargeType.RECURRING,
                "Cuota febrero", PERIOD, BigDecimal.ONE, new BigDecimal("100000.00"),
                new BigDecimal("100000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, null,
                null, CLOCK));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findPendingByCompanyIdAndSubscription(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, PERIOD.start(), PERIOD.end())).singleElement()
                .satisfies(charge -> {
                    assertThat(charge.getId()).isEqualTo(saved.getId());
                    assertThat(charge.getSubtotalAmount()).isEqualByComparingTo("100000.00");
                    assertThat(charge.getServicePeriod()).isEqualTo(PERIOD);
                });
        assertThat(repository.findByIdAndCompanyId(saved.getId(), SchemaSeed.OTRA_COMPANY_ID))
                .isEmpty();
    }

    /**
     * <b>La barandilla antiduplicados del barrido recurrente, contra SQL real.</b>
     *
     * <p>
     * No hay indice unico detras de esta regla —{@code subscription_charges} no
     * lleva columna de idempotencia— asi que la consulta <em>es</em> la barandilla:
     * si dejara de mirar una de sus cinco columnas, el barrido reiniciado
     * devengaria el mes dos veces y la factura saldria por el doble sin una sola
     * senal.
     */
    @Test
    @DisplayName("la llave detecta el cargo ya devengado de esa linea en ese periodo exacto")
    void la_llave_detecta_el_cargo_ya_devengado() {
        devengar(SchemaSeed.SUBSCRIPTION_ITEM_ID, PERIOD);

        assertThat(repository.existsRecurringCharge(RecurringChargeKey.of(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, PERIOD))).isTrue();
        // Otro periodo del mismo contrato y de la misma linea: no esta devengado.
        assertThat(repository.existsRecurringCharge(RecurringChargeKey.of(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID,
                new ServicePeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))))).isFalse();
        // Y ninguna clinica ve el devengo de otra.
        assertThat(
                repository.existsRecurringCharge(RecurringChargeKey.of(SchemaSeed.OTRA_COMPANY_ID,
                        SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, PERIOD)))
                .isFalse();
    }

    /**
     * El caso que obligo a que la llave llevara la <b>linea</b> y no el articulo:
     * con tramos acumulativos hay dos lineas vivas del mismo articulo en el mismo
     * periodo, y agruparlas dejaria de cobrar el segundo tramo.
     */
    @Test
    @DisplayName("el cargo de una linea no tapa el de otra linea del mismo periodo")
    void el_cargo_de_una_linea_no_tapa_el_de_otra() {
        devengar(SchemaSeed.SUBSCRIPTION_ITEM_ID, PERIOD);

        // La llave de la OTRA linea del mismo articulo, mismo contrato y mismo periodo.
        // Si la consulta agrupara por articulo, esto daria true y el segundo tramo
        // dejaria de cobrarse: media factura, en silencio y todos los meses.
        assertThat(repository.existsRecurringCharge(
                new RecurringChargeKey(SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                        SchemaSeed.SUBSCRIPTION_ITEM_ID + 1, PERIOD.start(), PERIOD.end())))
                .isFalse();
    }

    /**
     * <b>Sin filtro de estado, y esa es la mitad del valor.</b> El caso que cubre
     * es el barrido que murio DESPUES de emitir la factura: ahi el cargo ya esta
     * {@code INVOICED} y tiene que seguir contando como existente.
     */
    @Test
    @DisplayName("un cargo ya facturado sigue bloqueando el duplicado")
    void un_cargo_ya_facturado_sigue_bloqueando() {
        SubscriptionCharge cargo = devengar(SchemaSeed.SUBSCRIPTION_ITEM_ID, PERIOD);
        entityManager
                .createNativeQuery(
                        "UPDATE subscription_charges SET status = 'VOIDED' WHERE id = :id")
                .setParameter("id", cargo.getId()).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.existsRecurringCharge(RecurringChargeKey.of(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.SUBSCRIPTION_ITEM_ID, PERIOD))).isTrue();
    }

    private SubscriptionCharge devengar(Long itemId, ServicePeriod periodo) {
        SubscriptionCharge saved = repository.save(SubscriptionCharge.create(SchemaSeed.COMPANY_ID,
                SchemaSeed.SUBSCRIPTION_ID, itemId, ChargeType.RECURRING, "Cuota", periodo,
                BigDecimal.ONE, new BigDecimal("59000.00"), new BigDecimal("59000.00"),
                new BigDecimal("19.00"), TaxTreatment.TAXED, null, null, CLOCK));
        entityManager.flush();
        entityManager.clear();
        return saved;
    }
}
