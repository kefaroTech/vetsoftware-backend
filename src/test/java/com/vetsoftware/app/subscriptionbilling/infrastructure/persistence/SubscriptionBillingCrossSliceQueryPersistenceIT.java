package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Las dos consultas nativas con las que {@code subscriptionbilling} lee tablas
 * de otros slices — la condicion de cierre que el propio issue #404 se puso.
 *
 * <p>
 * <b>#404 dice literalmente:</b> «Si se decide <em>no</em> cambiarlo, entonces
 * hace falta que la rodaja ejecute de verdad estas dos consultas contra MySQL
 * real, porque hoy no las ejecuta nadie». Esto es eso.
 *
 * <p>
 * <b>Lo que estas consultas se juegan es el compilador.</b> Nombran
 * {@code subscriptions.enabled} y {@code platform_billing_config.singleton}
 * como texto plano dentro de un {@code createNativeQuery}: si una de esas
 * columnas cambia de nombre en el slice dueno, nada falla al compilar, nada
 * falla al arrancar el contexto, y el error aparece cuando un operador intenta
 * devengar un cargo o registrar una factura externa. Peor todavia en el caso de
 * {@code enabled}: si el slice {@code subscription} pasara a filtrar la baja de
 * otra forma —un {@code @SQLRestriction}, un estado— esta consulta seguiria
 * resolviendo como valido un contrato que su dueno considera inactivo, y se
 * devengarian cargos contra el. Ese fallo es <b>silencioso</b>, y es el motivo
 * de que aqui haya un caso que apaga el contrato y exige que deje de resolver.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("Lecturas cross-slice de subscriptionbilling — SQL nativo contra MySQL real (#404)")
class SubscriptionBillingCrossSliceQueryPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSubscriptionQueryPort subscriptionQueryPort;
    @Autowired
    private JpaBillingPolicyPort billingPolicyPort;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Nested
    @DisplayName("El contrato, siempre acotado por empresa")
    class ElContrato {

        @Test
        @DisplayName("resuelve el contrato propio y trae su empresa dentro del VO")
        void resuelve_el_contrato_propio_con_su_empresa() {
            assertThat(subscriptionQueryPort.findByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.COMPANY_ID)).contains(
                            new SubscriptionRef(SchemaSeed.SUBSCRIPTION_ID, SchemaSeed.COMPANY_ID));
        }

        @Test
        @DisplayName("el contrato de otra clinica no se resuelve: es lo que impide colgar un"
                + " cargo de la clinica A del contrato de la clinica B")
        void el_contrato_ajeno_no_se_resuelve() {
            assertThat(subscriptionQueryPort.findByIdAndCompanyId(SchemaSeed.OTRA_SUBSCRIPTION_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isPresent();

            assertThat(subscriptionQueryPort.findByIdAndCompanyId(SchemaSeed.OTRA_SUBSCRIPTION_ID,
                    SchemaSeed.COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("un contrato dado de baja deja de resolver: el filtro enabled = true es"
                + " texto plano y esta es la unica red que tiene")
        void un_contrato_dado_de_baja_deja_de_resolver() {
            entityManager
                    .createNativeQuery(
                            "UPDATE subscriptions SET enabled = false, version = version + 1"
                                    + " WHERE id = :id")
                    .setParameter("id", SchemaSeed.SUBSCRIPTION_ID).executeUpdate();

            assertThat(subscriptionQueryPort.findByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID,
                    SchemaSeed.COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("un contrato inexistente y las entradas nulas salen vacias sin reventar")
        void inexistente_y_nulos_salen_vacios() {
            assertThat(subscriptionQueryPort.findByIdAndCompanyId(8_999L, SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(subscriptionQueryPort.findByIdAndCompanyId(null, SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(subscriptionQueryPort.findByIdAndCompanyId(SchemaSeed.SUBSCRIPTION_ID, null))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("El plazo de pago de la plataforma")
    class ElPlazoDePago {

        @Test
        @DisplayName("lee el plazo de la fila singleton que siembra Liquibase, sin que el test"
                + " fije el valor: lo que se comprueba es que la columna existe y se lee")
        void lee_el_plazo_de_la_fila_singleton() {
            Number enLaTabla = (Number) entityManager
                    .createNativeQuery("SELECT c.default_payment_term_days"
                            + " FROM platform_billing_config c WHERE c.singleton = 1")
                    .getSingleResult();

            assertThat(billingPolicyPort.defaultPaymentTermDays()).isEqualTo(enLaTabla.intValue());
        }

        @Test
        @DisplayName("un plazo distinto se refleja: la consulta lee la fila, no una constante")
        void un_plazo_distinto_se_refleja() {
            entityManager.createNativeQuery(
                    "UPDATE platform_billing_config SET default_payment_term_days = 15,"
                            + " version = version + 1 WHERE singleton = 1")
                    .executeUpdate();

            assertThat(billingPolicyPort.defaultPaymentTermDays()).isEqualTo(15);
        }

        @Test
        @DisplayName("sin fila de configuracion falla legible en vez de inventarse un plazo:"
                + " un vencimiento que nadie decidio pone en mora a quien esta al dia")
        void sin_fila_de_configuracion_falla_legible() {
            entityManager.createNativeQuery("DELETE FROM platform_billing_config").executeUpdate();

            assertThatThrownBy(() -> billingPolicyPort.defaultPaymentTermDays())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("platform_billing_config has no row");
        }
    }
}
