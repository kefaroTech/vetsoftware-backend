package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Las dos consultas nativas de validacion de referencias, ejecutadas contra
 * MySQL real por primera vez.
 *
 * <p>
 * <b>Por que hacia falta esta rodaja.</b>
 * {@code JpaSubscriptionItemValidationPort} y
 * {@code JpaSubscriptionAmendmentValidationPort} son
 * {@code Jpa...ValidationPort}, y el propio CLAUDE.md deja ese naming <b>fuera
 * del alcance</b> de {@code ADAPTADOR_JPA_CON_RODAJA}. Es decir: nada en el
 * build obligaba a que su SQL llegara a ejecutarse antes de produccion. Las dos
 * consultas nombran tablas y columnas de <em>otro slice</em>
 * ({@code subscription_items}, {@code subscription_amendments}) como texto
 * plano; ni el compilador ni Hibernate miran dentro de un
 * {@code createNativeQuery}, asi que un nombre de columna equivocado no falla
 * hasta que un operador intenta devengar un cargo. Estos casos son lo unico que
 * comprueba que ese texto casa con el esquema que Liquibase acaba de crear.
 *
 * <p>
 * <b>Lo que se afirma no es "devuelve true".</b> Es que la consulta
 * <em>discrimina por empresa</em>: el mismo id existiendo en otra clinica tiene
 * que salir {@code false}. Un {@code AND i.company_id = :companyId} que se
 * perdiera en una edicion dejaria pasar la referencia ajena, y el cargo
 * quedaria colgado del contrato de otra clinica — que es exactamente el defecto
 * que las FK compuestas y este puerto existen para evitar.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("Puertos de validacion de referencias del contrato — SQL nativo contra MySQL real")
class SubscriptionReferenceValidationPersistenceIT extends AbstractDataJpaTest {

    private static final Long OTROSI_ID = 8_100L;
    private static final Long OTROSI_AJENO_ID = 8_101L;
    private static final Long ID_INEXISTENTE = 8_999L;

    @Autowired
    private JpaSubscriptionItemValidationPort itemValidationPort;
    @Autowired
    private JpaSubscriptionAmendmentValidationPort amendmentValidationPort;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Nested
    @DisplayName("Linea del contrato")
    class LineaDelContrato {

        @Test
        @DisplayName("la linea propia existe para su empresa")
        void la_linea_propia_existe_para_su_empresa() {
            assertThat(itemValidationPort.existsInCompany(SchemaSeed.SUBSCRIPTION_ITEM_ID,
                    SchemaSeed.COMPANY_ID)).isTrue();
        }

        @Test
        @DisplayName("la linea de otra clinica NO existe para esta empresa, aunque la fila si exista")
        void la_linea_ajena_no_existe_para_esta_empresa() {
            assertThat(itemValidationPort.existsInCompany(SchemaSeed.OTRO_SUBSCRIPTION_ITEM_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isTrue();

            assertThat(itemValidationPort.existsInCompany(SchemaSeed.OTRO_SUBSCRIPTION_ITEM_ID,
                    SchemaSeed.COMPANY_ID)).isFalse();
        }

        @Test
        @DisplayName("un id que no existe en ninguna empresa sale false sin reventar")
        void un_id_inexistente_sale_false() {
            assertThat(itemValidationPort.existsInCompany(ID_INEXISTENTE, SchemaSeed.COMPANY_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("no mira enabled: la FK compuesta que replica tampoco lo mira")
        void no_mira_enabled() {
            entityManager
                    .createNativeQuery(
                            "UPDATE subscription_items SET enabled = false WHERE id = :id")
                    .setParameter("id", SchemaSeed.SUBSCRIPTION_ITEM_ID).executeUpdate();

            assertThat(itemValidationPort.existsInCompany(SchemaSeed.SUBSCRIPTION_ITEM_ID,
                    SchemaSeed.COMPANY_ID)).isTrue();
        }
    }

    @Nested
    @DisplayName("Otrosi del contrato")
    class OtrosiDelContrato {

        @BeforeEach
        void otrosies() {
            otrosi(OTROSI_ID, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, "OTR-TEST-8100");
            otrosi(OTROSI_AJENO_ID, SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID,
                    "OTR-TEST-8101");
        }

        @Test
        @DisplayName("el otrosi propio existe para su empresa")
        void el_otrosi_propio_existe_para_su_empresa() {
            assertThat(amendmentValidationPort.existsInCompany(OTROSI_ID, SchemaSeed.COMPANY_ID))
                    .isTrue();
        }

        @Test
        @DisplayName("el otrosi de otra clinica NO existe para esta empresa: es el error de"
                + " dos pestanas abiertas con ids consecutivos")
        void el_otrosi_ajeno_no_existe_para_esta_empresa() {
            assertThat(amendmentValidationPort.existsInCompany(OTROSI_AJENO_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isTrue();

            assertThat(
                    amendmentValidationPort.existsInCompany(OTROSI_AJENO_ID, SchemaSeed.COMPANY_ID))
                    .isFalse();
        }

        @Test
        @DisplayName("un id que no existe en ninguna empresa sale false sin reventar")
        void un_id_inexistente_sale_false() {
            assertThat(
                    amendmentValidationPort.existsInCompany(ID_INEXISTENTE, SchemaSeed.COMPANY_ID))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Entradas nulas")
    class EntradasNulas {

        @Test
        @DisplayName("los dos puertos cortan en nulo sin llegar a la base")
        void cortan_en_nulo_sin_llegar_a_la_base() {
            assertThat(itemValidationPort.existsInCompany(null, SchemaSeed.COMPANY_ID)).isFalse();
            assertThat(itemValidationPort.existsInCompany(SchemaSeed.SUBSCRIPTION_ITEM_ID, null))
                    .isFalse();
            assertThat(amendmentValidationPort.existsInCompany(null, SchemaSeed.COMPANY_ID))
                    .isFalse();
            assertThat(amendmentValidationPort.existsInCompany(OTROSI_ID, null)).isFalse();
        }
    }

    /**
     * {@code chk_subscription_amendments_actor} exige exactamente un actor, y la
     * tabla no tiene ni {@code enabled} ni {@code version}: un otrosi no se retira,
     * se emite otro.
     */
    private void otrosi(Long id, Long companyId, Long subscriptionId, String numero) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_amendments (id, company_id, subscription_id,
                                                     amendment_number, amendment_type,
                                                     effective_date, reason,
                                                     requested_by_employee_id,
                                                     requested_by_system_user_id,
                                                     proration_amount,
                                                     monthly_delta_amount, quote_id,
                                                     client_request_id, created_date)
                VALUES (:id, :companyId, :subscriptionId, :numero, 'CHANGE_QUANTITY',
                        '2026-02-01', 'Alta de una linea', NULL, :systemUserId,
                        0.00, 0.00, NULL, :clientRequestId, NOW())
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("subscriptionId", subscriptionId).setParameter("numero", numero)
                .setParameter("systemUserId", SchemaSeed.SYSTEM_USER_ID)
                .setParameter("clientRequestId", "req-" + id).executeUpdate();
    }
}
