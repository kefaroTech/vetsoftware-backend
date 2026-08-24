package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CompanyAlreadyHasActiveSubscriptionException;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * La cabecera del contrato contra MySQL real. Lo que solo se puede comprobar
 * aquí: que «una empresa, un contrato vivo» lo impone de verdad
 * {@code uq_subscriptions_active_company} sobre la columna generada
 * {@code active_marker}, y que el adaptador traduce ese rechazo al conflicto de
 * negocio en vez de dejarlo salir como un 500.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionRepository — contrato vigente y tenant contra MySQL real")
class SubscriptionPersistenceIT extends AbstractDataJpaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN_PERIODO = LocalDate.of(2026, 3, 31);

    @Autowired
    private JpaSubscriptionRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    private static Subscription contratoEn(SubscriptionStatus status, Long companyId,
            String numero) {
        return new Subscription(null, numero, companyId, null, SchemaSeed.PRICE_LIST_ID,
                BillingCycle.MONTHLY, status, INICIO,
                status == SubscriptionStatus.TRIALING ? LocalDate.of(2026, 3, 10) : null, INICIO,
                FIN_PERIODO, FIN_PERIODO, null, 5, null, true, null, null, null, true);
    }

    @Nested
    @DisplayName("Una empresa, un contrato vivo")
    class UnContratoVivoPorEmpresa {

        @ParameterizedTest
        @EnumSource(value = SubscriptionStatus.class, names = {"TRIALING", "ACTIVE", "PAST_DUE",
                "READ_ONLY"})
        @DisplayName("un segundo contrato vigente sale como conflicto de negocio, no como 500")
        void unSegundoContratoVigenteEsConflicto(SubscriptionStatus status) {
            // Los cuatro estados de CURRENT llenan active_marker con el company_id, asi
            // que los cuatro chocan con el que ya tiene la empresa. Se recorren con
            // @EnumSource a proposito: si alguien anade un estado vigente y se olvida de
            // la columna generada, el contrato duplicado entraria en silencio.
            assertThatThrownBy(() -> repository
                    .save(contratoEn(status, SchemaSeed.COMPANY_ID, "SUS-TEST-000902")))
                    .isInstanceOf(CompanyAlreadyHasActiveSubscriptionException.class)
                    .hasMessageContaining(SchemaSeed.COMPANY_ID.toString());
        }

        @ParameterizedTest
        @EnumSource(value = SubscriptionStatus.class, names = {"CANCELLED", "EXPIRED"})
        @DisplayName("un contrato terminal sí convive con el vigente: es historia, no competencia")
        void unContratoTerminalConviveConElVigente(SubscriptionStatus status) {
            // active_marker vale NULL fuera de los cuatro estados vigentes, y MySQL
            // admite tantos NULL como haga falta en un indice unico. Es lo que permite
            // que una clinica que ya se fue una vez pueda volver a contratar.
            Subscription guardado = repository
                    .save(contratoEn(status, SchemaSeed.COMPANY_ID, "SUS-TEST-000903"));
            entityManager.flush();
            entityManager.clear();

            assertThat(guardado.getId()).isNotNull();
            assertThat(repository.findCurrentByCompanyId(SchemaSeed.COMPANY_ID)).get()
                    .extracting(Subscription::getId).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("otra empresa sí puede tener el suyo: el marcador es por empresa")
        void otraEmpresaTieneElSuyo() {
            assertThat(repository.findCurrentByCompanyId(SchemaSeed.OTRA_COMPANY_ID)).get()
                    .extracting(Subscription::getId).isEqualTo(SchemaSeed.OTRA_SUBSCRIPTION_ID);
        }
    }

    @Nested
    @DisplayName("Lecturas")
    class Lecturas {

        @Test
        @DisplayName("encuentra el contrato vigente y lo devuelve completo")
        void encuentraElContratoVigente() {
            assertThat(repository.findCurrentByCompanyId(SchemaSeed.COMPANY_ID)).get()
                    .satisfies(subscription -> {
                        assertThat(subscription.getId()).isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
                        assertThat(subscription.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
                        assertThat(subscription.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
                        assertThat(subscription.getGraceDays()).isEqualTo(5);
                        assertThat(subscription.isCurrent()).isTrue();
                    });
        }

        @Test
        @DisplayName("la vista de plataforma trae los contratos de todas las clínicas")
        void laVistaDePlataformaLosTraeTodos() {
            assertThat(repository.findAll(0, 50).content()).extracting(Subscription::getId)
                    .contains(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID);
        }

        @Test
        @DisplayName("el bloqueo pesimista devuelve la fila y respeta la empresa")
        void elBloqueoPesimistaRespetaLaEmpresa() {
            // lockByIdAndCompanyId es lo que serializa las altas y bajas de linea: sin
            // ese candado, la comprobacion de solape es un leer-y-luego-escribir y dos
            // transacciones concurrentes pasan las dos.
            assertThat(repository.lockByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.COMPANY_ID)).get().extracting(Subscription::getId)
                    .isEqualTo(SchemaSeed.SUBSCRIPTION_ID);
            assertThat(repository.lockByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("el lote del ciclo de vida avanza por id y se puede acotar")
        void elLoteDelCicloDeVidaAvanzaPorId() {
            // El barrido pagina por id y no por offset: con offset, una fila que cambia
            // de estado a mitad del barrido desplaza a las demas y alguna se salta.
            assertThat(repository.lockLifecycleBatchAfter(0L, 50)).extracting(Subscription::getId)
                    .contains(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID);
            assertThat(repository.lockLifecycleBatchAfter(SchemaSeed.SUBSCRIPTION_ID, 50))
                    .extracting(Subscription::getId).doesNotContain(SchemaSeed.SUBSCRIPTION_ID);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("ninguna lectura acotada devuelve el contrato de otra empresa")
        void ningunaLecturaCruzaDeEmpresa() {
            assertThat(repository.findByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).content())
                    .extracting(Subscription::getId).containsExactly(SchemaSeed.SUBSCRIPTION_ID);
            assertThat(repository.findAllByCompanyId(SchemaSeed.OTRA_COMPANY_ID, 0, 20).content())
                    .extracting(Subscription::getId)
                    .containsExactly(SchemaSeed.OTRA_SUBSCRIPTION_ID);
        }
    }
}
