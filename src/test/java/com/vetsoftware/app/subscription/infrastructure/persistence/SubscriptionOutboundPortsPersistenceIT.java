package com.vetsoftware.app.subscription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscription.application.dto.InitialCapacityTemplate;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.application.dto.PublishedCatalogItem;
import com.vetsoftware.app.subscription.application.dto.SubscriptionQuoteSnapshot;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.EmployeeRef;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Los puertos de salida del slice contra MySQL real.
 *
 * <p>
 * <b>Por qué existe esta rodaja.</b> {@code ADAPTADOR_JPA_CON_RODAJA} solo
 * exige red a los {@code Jpa<Algo>Repository}: los {@code ...QueryPort} y
 * {@code ...ValidationPort} quedan fuera del predicado a propósito. El efecto
 * secundario es que aquí viven las consultas menos ejercitadas del slice —una
 * JPQL con proyección posicional a {@code Object[]}, una nativa de cinco
 * {@code JOIN} y una que cruza al slice de cotizaciones—, y en ese estilo un
 * índice corrido o un alias que no case no lo dice el compilador: devuelve un
 * dato equivocado o un {@code null} en ejecución.
 */
@Import({JpaSubscriptionCommercialSnapshotPort.class, JpaSubscriptionQuoteSnapshotPort.class,
        JpaPlatformCatalogPort.class, JpaEmployeeQueryPort.class, JpaCompanyValidationPort.class,
        JpaCatalogItemValidationPort.class, JpaSubscriptionPriceListQueryPort.class,
        JpaSystemUserValidationPort.class})
@DisplayName("Puertos de salida de subscription — consultas contra MySQL real")
class SubscriptionOutboundPortsPersistenceIT extends AbstractDataJpaTest {

    private static final Long COTIZACION_ID = 9700L;
    private static final Long LINEA_COTIZACION_ID = 9701L;
    private static final Long CAPACIDAD_SEDE_ID = 9710L;
    private static final Long CAPACIDAD_USUARIO_ID = 9711L;
    private static final Long PRECIO_CAPACIDAD_SEDE_ID = 9712L;
    private static final Long PRECIO_CAPACIDAD_USUARIO_ID = 9713L;
    private static final LocalDate DIA = LocalDate.of(2026, 1, 15);

    @Autowired
    private JpaSubscriptionCommercialSnapshotPort commercialSnapshotPort;
    @Autowired
    private JpaSubscriptionQuoteSnapshotPort quoteSnapshotPort;
    @Autowired
    private JpaPlatformCatalogPort platformCatalogPort;
    @Autowired
    private JpaEmployeeQueryPort employeeQueryPort;
    @Autowired
    private JpaCompanyValidationPort companyValidationPort;
    @Autowired
    private JpaCatalogItemValidationPort catalogItemValidationPort;
    @Autowired
    private JpaSubscriptionPriceListQueryPort priceListQueryPort;
    @Autowired
    private JpaSystemUserValidationPort systemUserValidationPort;
    @PersistenceContext
    private EntityManager entityManager;

    /** Resuelto, no sembrado: el articulo CORE llega del changeset 308. */
    private Long nucleo;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        nucleo = SchemaSeed.catalogItemId(entityManager, "CORE");
    }

    private void configuracionDePlataforma() {
        entityManager.createNativeQuery("""
                INSERT INTO platform_billing_config (singleton, default_price_list_id,
                                                     default_grace_days, default_trial_days,
                                                     invoice_day_of_month,
                                                     default_payment_term_days,
                                                     created_date, version)
                VALUES (1, :priceListId, 7, 14, 1, 0, '2026-01-01 00:00:00', 0)
                ON DUPLICATE KEY UPDATE default_price_list_id = VALUES(default_price_list_id),
                                        default_grace_days    = VALUES(default_grace_days),
                                        default_trial_days    = VALUES(default_trial_days)
                """).setParameter("priceListId", SchemaSeed.PRICE_LIST_ID).executeUpdate();
    }

    private void cotizacionAceptada(Long companyId) {
        entityManager.createNativeQuery("""
                INSERT INTO quotes (id, quote_number, company_id, price_list_id, billing_cycle,
                                    subtotal_amount, discount_amount, tax_amount, total_amount,
                                    status, valid_until, trial_days, accepted_at,
                                    accepted_by_email, accepted_ip, client_request_id,
                                    created_date, enabled, version)
                VALUES (:id, 'COT-2026-09700', :companyId, :priceListId, 'MONTHLY',
                        100000.00, 0.00, 19000.00, 119000.00, 'ACCEPTED', '2026-12-31', 0,
                        '2026-01-10 09:00:00.000000', 'gerente@clinica.test', '203.0.113.9',
                        'quote-snapshot-1', '2026-01-01 00:00:00', true, 0)
                """).setParameter("id", COTIZACION_ID).setParameter("companyId", companyId)
                .setParameter("priceListId", SchemaSeed.PRICE_LIST_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO quote_lines (id, quote_id, catalog_item_id, line_number, item_code,
                                         item_name, item_type, quantity, contracted_quantity,
                                         included_quantity, unit_amount, discount_percent,
                                         discount_amount, discount_is_conditional,
                                         trial_eligibility, trial_outcome, trial_days,
                                         max_trial_days, tax_rate, tax_treatment, tax_amount,
                                         line_total, created_date, enabled)
                VALUES (:id, :quoteId, :catalogItemId, 1, 'CORE', 'Nucleo de prueba', 'MODULE',
                        1, 3, 2, 100000.00, 0.00, 0.00, false,
                        'NEVER_FREE', NULL, 0, 0, 19.00, 'TAXED', 19000.00, 119000.00,
                        '2026-01-01 00:00:00', true)
                """).setParameter("id", LINEA_COTIZACION_ID).setParameter("quoteId", COTIZACION_ID)
                .setParameter("catalogItemId", nucleo).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("El tramo publicado que se congela al firmar")
    class TramoPublicado {

        @Test
        @DisplayName("devuelve el artículo y el precio del tramo que aplica, campo por campo")
        void devuelveElTramoQueAplica() {
            // La consulta proyecta dieciocho columnas a un Object[] y las lee por
            // indice. Un indice corrido no lo dice el compilador: se lleva el
            // included_quantity de la columna de al lado y le regala -o le cobra-
            // unidades al cliente durante todo el contrato. Por eso se afirma campo a
            // campo y no solo que devuelva algo.
            Optional<PublishedCatalogItem> tramo = commercialSnapshotPort.findPublishedItem(
                    SchemaSeed.PRICE_LIST_ID, BillingCycle.MONTHLY, nucleo, 1, DIA);

            assertThat(tramo).get().satisfies(publicado -> {
                assertThat(publicado.catalogItemId()).isEqualTo(nucleo);
                assertThat(publicado.itemCode()).isEqualTo("CORE");
                assertThat(publicado.itemName()).isEqualTo("Clientes y mascotas");
                assertThat(publicado.itemType()).isEqualTo(SubscriptionItemType.MODULE);
                assertThat(publicado.capacityUnit()).isNull();
                // D-66: devuelve TODOS los tramos, no el que cubre la cantidad. El nucleo
                // no tiene escalones, asi que es uno solo y abierto por arriba.
                assertThat(publicado.tiers()).singleElement().satisfies(tier -> {
                    assertThat(tier.tierMin()).isEqualTo(1);
                    assertThat(tier.tierMax()).isNull();
                    assertThat(tier.includedQuantity()).isEqualTo(2);
                    assertThat(tier.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
                    assertThat(tier.unitAmount()).isEqualByComparingTo("100000.00");
                    assertThat(tier.taxRate()).isEqualByComparingTo("19.00");
                });
            });
        }

        @Test
        @DisplayName("no hay tramo para un ciclo de facturación que la tarifa no publica")
        void sinTramoParaOtroCiclo() {
            assertThat(commercialSnapshotPort.findPublishedItem(SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.ANNUAL, nucleo, 1, DIA)).isEmpty();
        }

        @Test
        @DisplayName("no hay tramo antes de que la tarifa entre en vigor")
        void sinTramoAntesDeLaVigencia() {
            // La lista publicada vale desde 2026-01-01. Firmar contra una tarifa que
            // todavia no ha entrado en vigor es firmar un precio que nadie aprobo.
            assertThat(commercialSnapshotPort.findPublishedItem(SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY, nucleo, 1, LocalDate.of(2025, 12, 31))).isEmpty();
        }

        @Test
        @DisplayName("sin día de referencia no se resuelve ningún tramo")
        void sinDiaDeReferencia() {
            assertThat(commercialSnapshotPort.findPublishedItem(SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY, nucleo, 1, null)).isEmpty();
        }

        @Test
        @DisplayName("una cantidad fuera del máximo del artículo no tiene tramo")
        void cantidadFueraDelMaximoDelArticulo() {
            // El nucleo se contrata una vez: min 1, max 1. Pedir dos no es un tramo mas
            // caro, es una peticion que el catalogo no admite.
            assertThat(commercialSnapshotPort.findPublishedItem(SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY, nucleo, 2, DIA)).isEmpty();
        }

        @Test
        @DisplayName("un artículo que no existe no tiene tramo")
        void articuloInexistente() {
            assertThat(commercialSnapshotPort.findPublishedItem(SchemaSeed.PRICE_LIST_ID,
                    BillingCycle.MONTHLY, -1L, 1, DIA)).isEmpty();
        }

        @Test
        @DisplayName("una tarifa que no existe no tiene tramo")
        void tarifaInexistente() {
            assertThat(commercialSnapshotPort.findPublishedItem(-1L, BillingCycle.MONTHLY, nucleo,
                    1, DIA)).isEmpty();
        }
    }

    @Nested
    @DisplayName("El mínimo estructural de la plataforma")
    class MinimoEstructural {

        @Test
        @DisplayName("resuelve el núcleo, su tarifa y los valores por defecto en una consulta")
        void resuelveElMinimoEstructural() {
            // Cinco JOIN nativos y una proyeccion por alias. Un alias que no case con
            // el getter de la interfaz devuelve null en ejecucion, y el contrato
            // inicial se firmaria con un precio nulo o sin dias de gracia.
            configuracionDePlataforma();
            entityManager.flush();
            entityManager.clear();

            Optional<InitialContractTemplate> plantilla = platformCatalogPort
                    .findInitialContractTemplate(BillingCycle.MONTHLY);

            assertThat(plantilla).get().satisfies(template -> {
                assertThat(template.priceListId()).isEqualTo(SchemaSeed.PRICE_LIST_ID);
                assertThat(template.catalogItemId()).isEqualTo(nucleo);
                assertThat(template.itemCode()).isEqualTo("CORE");
                assertThat(template.itemName()).isEqualTo("Clientes y mascotas");
                assertThat(template.itemType()).isEqualTo(SubscriptionItemType.MODULE);
                assertThat(template.capacityUnit()).isNull();
                assertThat(template.includedQuantity()).isEqualTo(2);
                assertThat(template.minQuantity()).isEqualTo(1);
                assertThat(template.unitAmount()).isEqualByComparingTo("100000.00");
                assertThat(template.taxRate()).isEqualByComparingTo("19.00");
                assertThat(template.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
                assertThat(template.defaultGraceDays()).isEqualTo(7);
                assertThat(template.defaultTrialDays()).isEqualTo(14);
            });
        }

        @Test
        @DisplayName("el núcleo es un MODULE y su unidad de capacidad se queda nula")
        void elNucleoNoLlevaUnidadDeCapacidad() {
            // El dominio rechaza una unidad colgada de un modulo. Si el adaptador
            // tradujera un null a un valor por defecto, el alta reventaria despues, en
            // el constructor de SubscriptionItem, con un mensaje que no señala aqui.
            configuracionDePlataforma();
            entityManager.flush();
            entityManager.clear();

            assertThat(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY)).get()
                    .satisfies(template -> assertThat(template.capacityUnit()).isNull());
        }

        @Test
        @DisplayName("sin ciclo de facturación no hay plantilla y no se consulta la base")
        void sinCicloNoHayPlantilla() {
            assertThat(platformCatalogPort.findInitialContractTemplate(null)).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(BillingCycle.class)
        @DisplayName("sin tarifa por defecto no hay plantilla para ningún ciclo")
        void sinTarifaPorDefectoNoHayPlantilla(BillingCycle ciclo) {
            // Este es el estado que dejan las migraciones: la fila de configuracion
            // existe -la siembra el mismo changeSet que crea la tabla- pero
            // default_price_list_id va nula porque todavia no habia ninguna lista a la
            // que apuntar. Falta una de las cinco piezas y la consulta no devuelve
            // fila, que es exactamente la respuesta que hace falta: mejor «no hay
            // plantilla» que un contrato inicial a medio armar.
            assertThat(platformCatalogPort.findInitialContractTemplate(ciclo)).isEmpty();
        }
    }

    /**
     * Siembra los articulos de capacidad del nucleo y su tramo publicado. Van con
     * {@code INSERT} normal y no con {@code INSERT IGNORE}: si una columna
     * {@code NOT NULL} se queda fuera, esto tiene que reventar aqui y no aparecer
     * mas tarde disfrazado de fila ausente.
     */
    private void capacidadesDelNucleo() {
        entityManager
                .createNativeQuery(
                        """
                                INSERT INTO catalog_items (id, code, name, item_type, capacity_unit, structural_minimum,
                                                           min_quantity, max_quantity, sort_order, status,
                                                           trial_eligibility, default_trial_days, trial_outcome,
                                                           service_nature, created_date, enabled, version)
                                VALUES (:sedeId, 'CAP_BRANCH', 'Sede incluida', 'CAPACITY', 'BRANCH', true,
                                        1, NULL, 5, 'ACTIVE', 'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                                        NOW(), true, 0),
                                       (:usuarioId, 'CAP_USER', 'Usuario incluido', 'CAPACITY', 'USER', true,
                                        1, NULL, 6, 'ACTIVE', 'NEVER_FREE', NULL, NULL, 'SOFTWARE_LICENSING',
                                        NOW(), true, 0)
                                """)
                .setParameter("sedeId", CAPACIDAD_SEDE_ID)
                .setParameter("usuarioId", CAPACIDAD_USUARIO_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO catalog_prices (id, price_list_id, catalog_item_id, billing_cycle,
                                            tier_min, tier_max, included_quantity, unit_amount,
                                            setup_amount, tax_rate, tax_treatment,
                                            created_date, enabled, version)
                VALUES (:precioSedeId, :priceListId, :sedeId, 'MONTHLY', 1, NULL, 0, 12000.00,
                        0.00, 19.00, 'TAXED', NOW(), true, 0),
                       (:precioUsuarioId, :priceListId, :usuarioId, 'MONTHLY', 1, NULL, 2,
                        9000.00, 0.00, 19.00, 'TAXED', NOW(), true, 0)
                """).setParameter("precioSedeId", PRECIO_CAPACIDAD_SEDE_ID)
                .setParameter("precioUsuarioId", PRECIO_CAPACIDAD_USUARIO_ID)
                .setParameter("priceListId", SchemaSeed.PRICE_LIST_ID)
                .setParameter("sedeId", CAPACIDAD_SEDE_ID)
                .setParameter("usuarioId", CAPACIDAD_USUARIO_ID).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Las capacidades del mínimo estructural (#490)")
    class CapacidadesDelMinimo {

        /**
         * <b>La prueba que habría cazado #490.</b> El catálogo sembrado tenía —y tiene—
         * seis artículos, todos {@code MODULE} o {@code BUNDLE}. La consulta del núcleo
         * devolvía su fila y nadie miraba más allá, así que el contrato inicial se
         * firmaba con una sola línea y {@code company_capacities} quedaba vacía: la
         * empresa nacía sin poder crear su propia sede principal. Con filas reales y
         * sin ningún doble, aquí eso es una lista vacía y se ve.
         */
        @Test
        @DisplayName("un catálogo de solo módulos no concede ninguna capacidad")
        void unCatalogoDeSoloModulosNoConcedeNada() {
            configuracionDePlataforma();
            entityManager.flush();
            entityManager.clear();

            assertThat(platformCatalogPort.findInitialContractTemplate(BillingCycle.MONTHLY))
                    .isPresent();
            assertThat(platformCatalogPort.findInitialCapacityTemplates(BillingCycle.MONTHLY))
                    .isEmpty();
        }

        @Test
        @DisplayName("trae una línea por artículo CAPACITY del núcleo, con su tarifa congelada")
        void traeUnaLineaPorCapacidadDelNucleo() {
            configuracionDePlataforma();
            capacidadesDelNucleo();

            List<InitialCapacityTemplate> capacidades = platformCatalogPort
                    .findInitialCapacityTemplates(BillingCycle.MONTHLY);

            assertThat(capacidades).hasSize(2).extracting(InitialCapacityTemplate::capacityUnit)
                    .containsExactly("BRANCH", "USER");
            assertThat(capacidades.get(0)).satisfies(sede -> {
                assertThat(sede.catalogItemId()).isEqualTo(CAPACIDAD_SEDE_ID);
                assertThat(sede.itemCode()).isEqualTo("CAP_BRANCH");
                assertThat(sede.itemName()).isEqualTo("Sede incluida");
                assertThat(sede.includedQuantity()).isZero();
                assertThat(sede.minQuantity()).isEqualTo(1);
                assertThat(sede.unitAmount()).isEqualByComparingTo("12000.00");
                assertThat(sede.taxRate()).isEqualByComparingTo("19.00");
                assertThat(sede.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
            });
            // included_quantity distinto entre las dos a proposito: es el campo que un
            // indice corrido o un alias mal escrito se lleva de la columna de al lado,
            // y con los dos a cero el error pasaria desapercibido.
            assertThat(capacidades.get(1).includedQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("una capacidad que no es del núcleo no entra en el mínimo")
        void laCapacidadQueNoEsDelNucleoNoEntra() {
            // structural_minimum es la pertenencia al minimo estructural. Una capacidad
            // vendible
            // aparte -mas terminales, mas almacenamiento- no se regala en el alta.
            configuracionDePlataforma();
            capacidadesDelNucleo();
            entityManager
                    .createNativeQuery(
                            "UPDATE catalog_items SET structural_minimum = false WHERE id = :id")
                    .setParameter("id", CAPACIDAD_USUARIO_ID).executeUpdate();
            entityManager.flush();
            entityManager.clear();

            assertThat(platformCatalogPort.findInitialCapacityTemplates(BillingCycle.MONTHLY))
                    .extracting(InitialCapacityTemplate::capacityUnit).containsExactly("BRANCH");
        }

        @Test
        @DisplayName("sin tramo publicado para el ciclo pedido no hay capacidad que firmar")
        void sinTramoParaElCicloNoHayCapacidad() {
            // La tarifa de laboratorio solo publica MONTHLY. Un alta anual no puede
            // firmar una capacidad cuyo precio nadie aprobo para ese ciclo.
            configuracionDePlataforma();
            capacidadesDelNucleo();

            assertThat(platformCatalogPort.findInitialCapacityTemplates(BillingCycle.ANNUAL))
                    .isEmpty();
        }

        @Test
        @DisplayName("sin ciclo de facturación no se consulta la base")
        void sinCicloNoSeConsulta() {
            assertThat(platformCatalogPort.findInitialCapacityTemplates(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("La proyección de la cotización aceptada")
    class ProyeccionDeLaCotizacion {

        @Test
        @DisplayName("trae la cabecera y sus líneas con la cantidad contratada, no la facturable")
        void traeCabeceraYLineas() {
            // contracted_quantity y no quantity: lo que se lleva al contrato es lo que
            // el cliente contrato, y la resta de lo incluido la rehace el propio
            // contrato. Si se copiara la facturable, un cliente con 3 usuarios y 2
            // incluidos aparecería con 1 contratado y perdería dos.
            cotizacionAceptada(SchemaSeed.COMPANY_ID);

            Optional<SubscriptionQuoteSnapshot> snapshot = quoteSnapshotPort
                    .findByIdAndCompanyId(COTIZACION_ID, SchemaSeed.COMPANY_ID);

            assertThat(snapshot).get().satisfies(quote -> {
                assertThat(quote.id()).isEqualTo(COTIZACION_ID);
                assertThat(quote.companyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                assertThat(quote.priceListId()).isEqualTo(SchemaSeed.PRICE_LIST_ID);
                assertThat(quote.billingCycle()).isEqualTo(BillingCycle.MONTHLY);
                assertThat(quote.accepted()).isTrue();
                assertThat(quote.acceptedBy()).isEqualTo("gerente@clinica.test");
                assertThat(quote.items()).singleElement().satisfies(item -> {
                    assertThat(item.catalogItemId()).isEqualTo(nucleo);
                    assertThat(item.itemCode()).isEqualTo("CORE");
                    assertThat(item.itemName()).isEqualTo("Nucleo de prueba");
                    assertThat(item.itemType()).isEqualTo(SubscriptionItemType.MODULE);
                    assertThat(item.capacityUnit()).isNull();
                    assertThat(item.includedQuantity()).isEqualTo(2);
                    assertThat(item.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
                    assertThat(item.quantity()).isEqualTo(3);
                    assertThat(item.unitAmount()).isEqualByComparingTo("100000.00");
                    assertThat(item.taxRate()).isEqualByComparingTo("19.00");
                });
            });
        }

        @Test
        @DisplayName("no devuelve la cotización de otra empresa")
        void noDevuelveLaDeOtraEmpresa() {
            // Es la puerta por la que un contrato se firmaria con las condiciones
            // negociadas por otra clinica. La consulta acota por company_id en la
            // cabecera Y en las lineas, no solo en la cabecera.
            cotizacionAceptada(SchemaSeed.COMPANY_ID);

            assertThat(quoteSnapshotPort.findByIdAndCompanyId(COTIZACION_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("una cotización que no existe no da proyección")
        void cotizacionInexistente() {
            assertThat(quoteSnapshotPort.findByIdAndCompanyId(-1L, SchemaSeed.COMPANY_ID))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Quién firma y qué existe")
    class QuienFirmaYQueExiste {

        @Test
        @DisplayName("el empleado que firma tiene que ser de la misma empresa que el contrato")
        void elEmpleadoEsDeLaMismaEmpresa() {
            // R14. La FK del otrosi apunta a employees a secas, asi que la base NO
            // impide firmar con el empleado de otra clinica: lo impide este filtro.
            assertThat(employeeQueryPort.findByIdAndCompanyId(SchemaSeed.EMPLOYEE_ID,
                    SchemaSeed.COMPANY_ID)).get()
                    .isEqualTo(new EmployeeRef(SchemaSeed.EMPLOYEE_ID, "Ana Ruiz"));
            assertThat(employeeQueryPort.findByIdAndCompanyId(SchemaSeed.EMPLOYEE_ID,
                    SchemaSeed.OTRA_COMPANY_ID)).isEmpty();
        }

        @Test
        @DisplayName("sin empleado o sin empresa no se resuelve nadie, y no se consulta la base")
        void sinEmpleadoNiEmpresa() {
            assertThat(employeeQueryPort.findByIdAndCompanyId(null, SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(employeeQueryPort.findByIdAndCompanyId(SchemaSeed.EMPLOYEE_ID, null))
                    .isEmpty();
            assertThat(employeeQueryPort.findByIdAndCompanyId(-1L, SchemaSeed.COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("las validaciones de existencia aceptan lo sembrado")
        void lasCuatroValidacionesAceptanLoSembrado() {
            assertThatCode(() -> {
                companyValidationPort.validateExists(SchemaSeed.COMPANY_ID);
                catalogItemValidationPort.validateExists(nucleo);
                systemUserValidationPort.validateExists(SchemaSeed.SYSTEM_USER_ID);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("la tarifa sembrada vuelve publicada y con su ventana, no como un booleano")
        void laTarifaVuelveConSuVentana() {
            // Sustituye al antiguo existsById: la cabecera del contrato necesita saber
            // si esta PUBLICADA y entre que fechas, no solo si la fila existe (D-73).
            assertThat(priceListQueryPort.findPublishedById(SchemaSeed.PRICE_LIST_ID))
                    .hasValueSatisfying(tarifa -> {
                        assertThat(tarifa.id()).isEqualTo(SchemaSeed.PRICE_LIST_ID);
                        assertThat(tarifa.validFrom()).isNotNull();
                    });
        }

        @Test
        @DisplayName("una empresa que no existe se rechaza nombrándola")
        void empresaInexistente() {
            assertThatThrownBy(() -> companyValidationPort.validateExists(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: -1");
            assertThatThrownBy(() -> companyValidationPort.validateExists(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found");
        }

        @Test
        @DisplayName("un artículo, una tarifa o una cuenta de plataforma inexistentes se rechazan")
        void referenciasInexistentes() {
            assertThatThrownBy(() -> catalogItemValidationPort.validateExists(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Catalog item not found: -1");
            assertThatThrownBy(() -> catalogItemValidationPort.validateExists(null))
                    .isInstanceOf(IllegalArgumentException.class);
            // La tarifa no lanza: devuelve vacio. Un id inexistente y una lista en
            // borrador son el mismo vacio aqui; quien distingue caducada de ausente es
            // el caso de uso, que compara la ventana contra su reloj zonado.
            assertThat(priceListQueryPort.findPublishedById(-1L)).isEmpty();
            assertThat(priceListQueryPort.findPublishedById(null)).isEmpty();
            assertThatThrownBy(() -> systemUserValidationPort.validateExists(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("System user not found: -1");
            assertThatThrownBy(() -> systemUserValidationPort.validateExists(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
