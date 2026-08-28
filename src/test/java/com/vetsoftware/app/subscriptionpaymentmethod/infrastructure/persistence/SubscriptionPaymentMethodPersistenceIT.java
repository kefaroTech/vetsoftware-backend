package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.MandateStatus;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de {@link JpaSubscriptionPaymentMethodRepository}
 * contra MySQL real.
 *
 * <p>
 * <strong>Lo que solo se puede probar aqui es {@code default_marker}.</strong>
 * Es una columna generada ({@code CASE WHEN is_default = TRUE AND
 * mandate_status = 'ACTIVE' THEN company_id ELSE NULL END}) sobre la que cuelga
 * {@code uq_subscription_payment_methods_default}, y es lo unico que garantiza
 * «un solo medio de pago predeterminado por empresa». No esta mapeada en la
 * entidad —mapearla haria que Hibernate la nombrara en el INSERT y MySQL
 * rechazaria <em>todas</em> las altas—, asi que ningun test con dobles la ve.
 * Los cuatro casos de {@code Predeterminado} congelan justo lo que el changeset
 * 319 declara: choca dentro de la empresa, convive entre empresas, y un mandato
 * que deja de estar {@code ACTIVE} —revocado o caducado— libera el hueco sin
 * borrar el rastro de cual lo fue.
 *
 * <p>
 * Los {@code CHECK} de forma se prueban construyendo la entidad JPA a mano y no
 * por el adaptador: el dominio ya rechaza esas combinaciones antes de llegar al
 * motor, y lo que hay que comprobar es que la base tambien las para si algun
 * dia entra una escritura por otro camino.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSubscriptionPaymentMethodRepository — medios de pago contra MySQL real")
class SubscriptionPaymentMethodPersistenceIT extends AbstractDataJpaTest {

    private static final Long EMPRESA = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_EMPRESA = SchemaSeed.OTRA_COMPANY_ID;

    private static final String PASARELA = "wompi";
    private static final String CONSTANCIA = "acta-mandato-2026-0447";

    /**
     * Tres marcas de tiempo distintas y reconocibles: si dos se intercambian, rojo.
     */
    private static final LocalDateTime AUTORIZADO_EN = LocalDateTime.of(2026, 3, 4, 8, 15, 30);
    private static final LocalDateTime REVOCADO_EN = LocalDateTime.of(2026, 5, 6, 21, 45, 10);
    private static final LocalDateTime CREADO_EN = LocalDateTime.of(2026, 4, 5, 13, 20, 0);

    private static final LocalDate VENCE_EN = LocalDate.of(2027, 9, 30);

    @Autowired
    private JpaSubscriptionPaymentMethodRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda y recupera el medio de pago con cada campo en su sitio")
        void guarda_y_recupera_el_medio_de_pago_con_cada_campo_en_su_sitio() {
            Long id = guardar(new SubscriptionPaymentMethod(null, EMPRESA, PaymentMethodKind.CARD,
                    PASARELA, "tok_test_7f3a", "VISA", "4242", VENCE_EN, MandateStatus.REVOKED,
                    CONSTANCIA, AUTORIZADO_EN, REVOCADO_EN, "El cliente cambio de banco", false,
                    CREADO_EN, true, null)).getId();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(id, EMPRESA)).hasValueSatisfying(medio -> {
                assertThat(medio.getCompanyId()).isEqualTo(EMPRESA);
                assertThat(medio.getMethodKind()).isEqualTo(PaymentMethodKind.CARD);
                assertThat(medio.getGateway()).isEqualTo(PASARELA);
                assertThat(medio.getToken()).isEqualTo("tok_test_7f3a");
                assertThat(medio.getBrand()).isEqualTo("VISA");
                assertThat(medio.getLastFour()).isEqualTo("4242");
                assertThat(medio.getExpiresOn()).isEqualTo(VENCE_EN);
                assertThat(medio.getMandateStatus()).isEqualTo(MandateStatus.REVOKED);
                assertThat(medio.getMandateEvidence()).isEqualTo(CONSTANCIA);
                assertThat(medio.getAuthorizedAt()).isEqualTo(AUTORIZADO_EN);
                assertThat(medio.getRevokedAt()).isEqualTo(REVOCADO_EN);
                assertThat(medio.getRevokedReason()).isEqualTo("El cliente cambio de banco");
                assertThat(medio.isDefaultMethod()).isFalse();
                assertThat(medio.getCreatedDate()).isEqualTo(CREADO_EN);
                assertThat(medio.isEnabled()).isTrue();
                assertThat(medio.getVersion()).isNotNull();
            });
        }

        @Test
        @DisplayName("localiza el medio por el par pasarela y testigo, que es unicidad global")
        void localiza_el_medio_por_el_par_pasarela_y_testigo() {
            Long id = guardar(tarjeta(EMPRESA, "tok-buscado", VENCE_EN)).getId();
            entityManager.clear();

            assertThat(repository.findByGatewayAndToken(PASARELA, "tok-buscado"))
                    .hasValueSatisfying(medio -> assertThat(medio.getId()).isEqualTo(id));
            assertThat(repository.findByGatewayAndToken(PASARELA, "tok-inexistente")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la carga por id acotada no cruza de empresa")
        void la_carga_por_id_acotada_no_cruza_de_empresa() {
            Long id = guardar(tarjeta(EMPRESA, "tok-propio", VENCE_EN)).getId();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(id, EMPRESA)).isPresent();
            assertThat(repository.findByIdAndCompanyId(id, OTRA_EMPRESA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Predeterminado")
    class Predeterminado {

        @Test
        @DisplayName("dos medios activos predeterminados de la misma empresa no caben")
        void dos_medios_activos_predeterminados_de_la_misma_empresa_no_caben() {
            guardar(predeterminada(EMPRESA, "tok-primera"));

            // Testigos distintos a proposito: con el mismo saltaria uq_..._token
            // primero y el caso pasaria por el motivo equivocado.
            EngineConstraint.assertViolates("uq_subscription_payment_methods_default", () -> {
                repository.save(predeterminada(EMPRESA, "tok-segunda"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos empresas distintas pueden tener cada una su predeterminado")
        void dos_empresas_distintas_pueden_tener_cada_una_su_predeterminado() {
            Long propio = guardar(predeterminada(EMPRESA, "tok-propio")).getId();
            Long ajeno = guardar(predeterminada(OTRA_EMPRESA, "tok-ajeno")).getId();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(propio, EMPRESA))
                    .hasValueSatisfying(medio -> assertThat(medio.isDefaultMethod()).isTrue());
            assertThat(repository.findByIdAndCompanyId(ajeno, OTRA_EMPRESA))
                    .hasValueSatisfying(medio -> assertThat(medio.isDefaultMethod()).isTrue());
        }

        @Test
        @DisplayName("un predeterminado revocado libera el hueco para otro de la misma empresa")
        void un_predeterminado_revocado_libera_el_hueco() {
            Long revocado = guardar(new SubscriptionPaymentMethod(null, EMPRESA,
                    PaymentMethodKind.CARD, PASARELA, "tok-revocado", "VISA", "4242", VENCE_EN,
                    MandateStatus.REVOKED, CONSTANCIA, AUTORIZADO_EN, REVOCADO_EN,
                    "El cliente cambio de banco", true, CREADO_EN, true, null)).getId();

            Long nuevo = guardar(predeterminada(EMPRESA, "tok-nuevo")).getId();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(nuevo, EMPRESA))
                    .hasValueSatisfying(medio -> {
                        assertThat(medio.isDefaultMethod()).isTrue();
                        assertThat(medio.getMandateStatus()).isEqualTo(MandateStatus.ACTIVE);
                    });
            // El rastro de cual lo fue sigue ahi: is_default = true sobre un mandato
            // revocado, que es exactamente lo que default_marker deja de proyectar.
            assertThat(repository.findByIdAndCompanyId(revocado, EMPRESA))
                    .hasValueSatisfying(medio -> {
                        assertThat(medio.isDefaultMethod()).isTrue();
                        assertThat(medio.getMandateStatus()).isEqualTo(MandateStatus.REVOKED);
                    });
        }

        @Test
        @DisplayName("un predeterminado caducado libera el hueco para otro de la misma empresa")
        void un_predeterminado_caducado_libera_el_hueco() {
            Long caducado = guardar(new SubscriptionPaymentMethod(null, EMPRESA,
                    PaymentMethodKind.CARD, PASARELA, "tok-caducado", "MASTERCARD", "1881",
                    LocalDate.of(2026, 1, 31), MandateStatus.EXPIRED, CONSTANCIA, AUTORIZADO_EN,
                    null, null, true, CREADO_EN, true, null)).getId();

            Long nuevo = guardar(predeterminada(EMPRESA, "tok-nuevo")).getId();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(nuevo, EMPRESA))
                    .hasValueSatisfying(medio -> assertThat(medio.isDefaultMethod()).isTrue());
            assertThat(repository.findByIdAndCompanyId(caducado, EMPRESA))
                    .hasValueSatisfying(medio -> {
                        assertThat(medio.isDefaultMethod()).isTrue();
                        assertThat(medio.getMandateStatus()).isEqualTo(MandateStatus.EXPIRED);
                    });
        }

        @Test
        @DisplayName("liberar el predeterminado solo toca a la empresa indicada y mueve su version")
        void liberar_el_predeterminado_solo_toca_a_la_empresa_indicada() {
            Long propio = guardar(predeterminada(EMPRESA, "tok-propio")).getId();
            Long ajeno = guardar(predeterminada(OTRA_EMPRESA, "tok-ajeno")).getId();
            Long entrante = guardar(tarjeta(EMPRESA, "tok-entrante", VENCE_EN)).getId();
            entityManager.clear();

            Long versionPropiaAntes = repository.findByIdAndCompanyId(propio, EMPRESA).orElseThrow()
                    .getVersion();
            Long versionAjenaAntes = repository.findByIdAndCompanyId(ajeno, OTRA_EMPRESA)
                    .orElseThrow().getVersion();

            int afectados = repository.clearDefaultForCompany(EMPRESA, entrante);

            assertThat(afectados).isEqualTo(1);
            assertThat(repository.findByIdAndCompanyId(propio, EMPRESA))
                    .hasValueSatisfying(medio -> {
                        assertThat(medio.isDefaultMethod()).isFalse();
                        assertThat(medio.getVersion()).isEqualTo(versionPropiaAntes + 1);
                    });
            assertThat(repository.findByIdAndCompanyId(ajeno, OTRA_EMPRESA))
                    .hasValueSatisfying(medio -> {
                        assertThat(medio.isDefaultMethod()).isTrue();
                        assertThat(medio.getVersion()).isEqualTo(versionAjenaAntes);
                    });
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class Restricciones {

        @Test
        @DisplayName("el mismo testigo de la misma pasarela no se registra dos veces")
        void el_mismo_testigo_de_la_misma_pasarela_no_se_registra_dos_veces() {
            guardar(tarjeta(EMPRESA, "tok-repetido", VENCE_EN));

            // De otra empresa: la unicidad de (gateway, token) es global, no por empresa.
            EngineConstraint.assertViolates("uq_subscription_payment_methods_token", () -> {
                repository.save(tarjeta(OTRA_EMPRESA, "tok-repetido", VENCE_EN));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("una tarjeta sin los ultimos cuatro digitos no entra")
        void una_tarjeta_sin_los_ultimos_cuatro_digitos_no_entra() {
            SubscriptionPaymentMethodJpaEntity entidad = filaTarjeta("tok-sin-cuatro");
            entidad.setLastFour(null);

            EngineConstraint.assertViolates("chk_subscription_payment_methods_card_shape", () -> {
                entityManager.persist(entidad);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("un PSE con fecha de vencimiento no entra: un debito automatico no caduca")
        void un_pse_con_fecha_de_vencimiento_no_entra() {
            SubscriptionPaymentMethodJpaEntity entidad = fila(PaymentMethodKind.PSE,
                    "tok-pse-vence");
            entidad.setExpiresOn(VENCE_EN);

            EngineConstraint.assertViolates("chk_subscription_payment_methods_card_shape", () -> {
                entityManager.persist(entidad);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("una revocacion anterior a la autorizacion no entra")
        void una_revocacion_anterior_a_la_autorizacion_no_entra() {
            SubscriptionPaymentMethodJpaEntity entidad = filaTarjeta("tok-revocado-antes");
            entidad.setMandateStatus(MandateStatus.REVOKED);
            entidad.setRevokedAt(AUTORIZADO_EN.minusDays(1));
            entidad.setRevokedReason("El cliente cambio de banco");

            EngineConstraint.assertViolates("chk_subscription_payment_methods_revocation", () -> {
                entityManager.persist(entidad);
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("unos ultimos cuatro que no son cuatro digitos no entran")
        void unos_ultimos_cuatro_que_no_son_cuatro_digitos_no_entran() {
            SubscriptionPaymentMethodJpaEntity entidad = filaTarjeta("tok-cuatro-malos");
            entidad.setLastFour("12ab");

            EngineConstraint.assertViolates("chk_subscription_payment_methods_last_four", () -> {
                entityManager.persist(entidad);
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("el barrido de vencimientos devuelve solo las tarjetas vivas dentro de la"
                + " ventana, de todas las empresas y por fecha de caducidad")
        void el_barrido_de_vencimientos_devuelve_solo_las_tarjetas_vivas_dentro_de_la_ventana() {
            Long pronto = guardar(tarjeta(EMPRESA, "tok-pronto", LocalDate.of(2026, 9, 30)))
                    .getId();
            Long ajena = guardar(tarjeta(OTRA_EMPRESA, "tok-ajena", LocalDate.of(2026, 11, 15)))
                    .getId();
            guardar(tarjeta(EMPRESA, "tok-lejana", LocalDate.of(2027, 3, 31)));
            guardar(new SubscriptionPaymentMethod(null, EMPRESA, PaymentMethodKind.CARD, PASARELA,
                    "tok-caducada", "VISA", "4242", LocalDate.of(2026, 8, 1), MandateStatus.EXPIRED,
                    CONSTANCIA, AUTORIZADO_EN, null, null, false, CREADO_EN, true, null));
            guardar(SubscriptionPaymentMethod.register(EMPRESA, PaymentMethodKind.PSE, PASARELA,
                    "tok-pse", null, null, null, CONSTANCIA, AUTORIZADO_EN, CREADO_EN));
            entityManager.clear();

            PageResult<SubscriptionPaymentMethod> pagina = repository
                    .findAllExpiringBefore(LocalDate.of(2026, 12, 1), 0, 20);

            assertThat(pagina.content()).extracting(SubscriptionPaymentMethod::getId)
                    .containsExactly(pronto, ajena);
            assertThat(pagina.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("el listado de la empresa pone el predeterminado primero y desempata por id")
        void el_listado_de_la_empresa_pone_el_predeterminado_primero_y_desempata_por_id() {
            SubscriptionPaymentMethod antiguoPredeterminado = tarjetaAutorizadaEn(EMPRESA,
                    "tok-predeterminado", LocalDateTime.of(2026, 1, 10, 9, 0));
            antiguoPredeterminado.makeDefault();
            Long predeterminado = guardar(antiguoPredeterminado).getId();

            LocalDateTime mismoInstante = LocalDateTime.of(2026, 6, 20, 7, 0);
            Long primero = guardar(tarjetaAutorizadaEn(EMPRESA, "tok-primero", mismoInstante))
                    .getId();
            Long segundo = guardar(tarjetaAutorizadaEn(EMPRESA, "tok-segundo", mismoInstante))
                    .getId();
            guardar(tarjeta(OTRA_EMPRESA, "tok-ajeno", VENCE_EN));
            entityManager.clear();

            PageResult<SubscriptionPaymentMethod> pagina = repository.findAllByCompanyId(EMPRESA, 0,
                    20);

            assertThat(pagina.content()).extracting(SubscriptionPaymentMethod::getId)
                    .containsExactly(predeterminado, segundo, primero);
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }

        @Test
        @DisplayName("el listado de plataforma cruza empresas y conserva el mismo orden")
        void el_listado_de_plataforma_cruza_empresas_y_conserva_el_mismo_orden() {
            Long propio = guardar(predeterminada(EMPRESA, "tok-propio")).getId();
            Long ajeno = guardar(tarjeta(OTRA_EMPRESA, "tok-ajeno", VENCE_EN)).getId();
            entityManager.clear();

            PageResult<SubscriptionPaymentMethod> pagina = repository.findAll(0, 20);

            assertThat(pagina.content()).extracting(SubscriptionPaymentMethod::getId)
                    .containsExactly(propio, ajeno);
            assertThat(pagina.content()).extracting(SubscriptionPaymentMethod::getCompanyId)
                    .containsExactly(EMPRESA, OTRA_EMPRESA);
        }
    }

    private SubscriptionPaymentMethod guardar(SubscriptionPaymentMethod medio) {
        SubscriptionPaymentMethod guardado = repository.save(medio);
        entityManager.flush();
        return guardado;
    }

    private static SubscriptionPaymentMethod tarjeta(Long empresa, String testigo,
            LocalDate venceEn) {
        return SubscriptionPaymentMethod.register(empresa, PaymentMethodKind.CARD, PASARELA,
                testigo, "VISA", "4242", venceEn, CONSTANCIA, AUTORIZADO_EN, CREADO_EN);
    }

    private static SubscriptionPaymentMethod tarjetaAutorizadaEn(Long empresa, String testigo,
            LocalDateTime autorizadoEn) {
        return SubscriptionPaymentMethod.register(empresa, PaymentMethodKind.CARD, PASARELA,
                testigo, "VISA", "4242", VENCE_EN, CONSTANCIA, autorizadoEn, CREADO_EN);
    }

    private static SubscriptionPaymentMethod predeterminada(Long empresa, String testigo) {
        SubscriptionPaymentMethod medio = tarjeta(empresa, testigo, VENCE_EN);
        medio.makeDefault();
        return medio;
    }

    /**
     * Fila construida a mano, sin pasar por el dominio: los {@code CHECK} de forma
     * los prueba solo quien puede escribir una combinacion que el dominio rechaza.
     * {@code default_marker} no se toca —es generada— y {@code version} la siembra
     * Hibernate.
     */
    private static SubscriptionPaymentMethodJpaEntity fila(PaymentMethodKind tipo, String testigo) {
        SubscriptionPaymentMethodJpaEntity entidad = new SubscriptionPaymentMethodJpaEntity();
        entidad.setCompanyId(EMPRESA);
        entidad.setMethodKind(tipo);
        entidad.setGateway(PASARELA);
        entidad.setToken(testigo);
        entidad.setMandateStatus(MandateStatus.ACTIVE);
        entidad.setMandateEvidence(CONSTANCIA);
        entidad.setAuthorizedAt(AUTORIZADO_EN);
        entidad.setDefaultMethod(false);
        entidad.setCreatedDate(CREADO_EN);
        entidad.setEnabled(true);
        return entidad;
    }

    private static SubscriptionPaymentMethodJpaEntity filaTarjeta(String testigo) {
        SubscriptionPaymentMethodJpaEntity entidad = fila(PaymentMethodKind.CARD, testigo);
        entidad.setBrand("VISA");
        entidad.setLastFour("4242");
        entidad.setExpiresOn(VENCE_EN);
        return entidad;
    }
}
