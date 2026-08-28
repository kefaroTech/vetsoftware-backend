package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import com.vetsoftware.app.documentwithholding.domain.WithholdingType;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaDocumentWithholdingRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar son las barandillas que solo el
 * motor tiene</b>, y la mas interesante es la unicidad por caso. Las otras
 * rodajas comprueban que el dominio replica los {@code CHECK}; aqui se
 * comprueba lo que el dominio <i>no puede</i> saber: que dos retenciones del
 * mismo documento y del mismo tipo chocan aunque ninguna de las dos tenga
 * municipio. Eso funciona solo gracias al centinela de {@code municipality_key}
 * —una columna generada que sustituye el vacio por {@code '-'}— porque en un
 * indice unico de MySQL dos {@code NULL} no chocan entre si y las dos filas
 * habrian cabido.
 *
 * <p>
 * <b>El adaptador se construye a mano y no se inyecta.</b>
 * {@code PersistenceSliceConfig} enumera los adaptadores de cada rodaja y esta
 * feature todavia no figura ahi; anadirla es una edicion de infraestructura de
 * test compartida, que es exactamente lo que esta tarea tiene prohibido tocar.
 * Construirlo con {@code new} sobre el {@code DocumentWithholdingJpaRepository}
 * que {@code @DataJpaTest} ya registra da el mismo comportamiento —el SQL que
 * se ejercita es el real— y ademas <b>no cambia la clave del
 * {@code MergedContextConfiguration}</b>: el {@code @Import} sigue siendo
 * unicamente {@code PersistenceSliceConfig}, asi que esta clase comparte el
 * contexto cacheado con las otras noventa rodajas en vez de pagar un arranque
 * entero. Ver el informe: queda pendiente registrarlo alli.
 *
 * <p>
 * <b>El seed no trae tablas fiscales.</b> {@code SchemaSeed} satisface claves
 * foraneas y se detiene antes; la factura, el certificado y el codigo DIVIPOLA
 * del municipio se insertan con SQL nativo y con ids del rango 8400, que
 * ninguna otra rodaja usa.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaDocumentWithholdingRepository — retenciones contra MySQL real")
class DocumentWithholdingPersistenceIT extends AbstractDataJpaTest {

    private static final Long FACTURA_PROPIA = 8400L;
    private static final Long FACTURA_AJENA = 8401L;

    /**
     * Segunda factura de la misma empresa. Hace falta porque
     * <b>{@code uq_document_withholdings_case} NO incluye el ano</b>: su clave es
     * {@code (company_id, billing_document_id, withholding_type, municipality_key)}.
     * Dos retenciones de renta del mismo documento chocan aunque sean de ejercicios
     * distintos, que es correcto —una factura pertenece a un solo periodo— y por
     * eso el caso del ano anterior necesita colgar de otra.
     */
    private static final Long FACTURA_PROPIA_ANTERIOR = 8402L;

    /**
     * El periodo facturado de las dos facturas propias, y <b>no puede ser el
     * mismo</b>.
     *
     * <p>
     * {@code uq_sbd_recurring_cycle} (changeset 249) es unico sobre
     * {@code (subscription_id, period_start, period_end)} para las facturas de
     * {@code billing_reason = 'RECURRING_CYCLE'}, y las dos facturas propias
     * cuelgan de la misma suscripcion del seed. Con el mismo periodo en las dos, el
     * fixture choca contra el esquema
     * —{@code Duplicate entry '970-2026-02-01-2026-02-28'}— y se lleva por delante
     * el {@code @BeforeEach} entero.
     *
     * <p>
     * La segunda es, por definicion, la del ejercicio anterior: su periodo es el
     * del ano pasado, que es lo que el caso que la usa esta representando.
     */
    private static final String PERIODO_INICIO = "2026-02-01";
    private static final String PERIODO_FIN = "2026-02-28";
    private static final String PERIODO_ANTERIOR_INICIO = "2025-02-01";
    private static final String PERIODO_ANTERIOR_FIN = "2025-02-28";
    private static final Long CERTIFICADO_PROPIO = 8410L;
    private static final Long CERTIFICADO_AJENO = 8411L;

    /**
     * Codigo DIVIPOLA que se le pone al municipio del seed, que nace sin ninguno.
     *
     * <p>
     * <strong>No puede ser un codigo real.</strong> Liquibase siembra la geografia
     * de Colombia entera —{@code 022_seed_americas_geography} crea los municipios y
     * {@code 114_backfill_dane_code_colombia} les escribe su DIVIPOLA— y desde
     * {@code 315_align_city_dane_code_for_withholdings} la columna lleva
     * {@code uq_cities_dane_code}, que es global. {@code 05001} es
     * <em>Medellin</em> y ya existe en {@code cities} antes de que esta rodaja
     * arranque: pisarselo al municipio del seed reventaba las diecisiete pruebas de
     * esta clase con
     * {@code Duplicate entry '05001' for key 'cities.uq_cities_dane_code'}. El dato
     * de la migracion es correcto; el que competia con el era el fixture.
     *
     * <p>
     * El rango {@code 00xxx} es seguro por construccion —no existe departamento
     * {@code 00} en DIVIPOLA, asi que ninguna migracion lo siembra ni lo sembrara—
     * y sigue cumpliendo {@code chk_cities_dane_code}, que exige cinco digitos. Es
     * el mismo criterio que ya aplico {@code CityPersistenceIT}; el {@code 00900}
     * ademas no se cruza con su contador, que reparte desde {@code 00001}.
     */
    private static final String MUNICIPIO = "00900";

    /**
     * Tres fechas deliberadamente distintas. Si el mapper cruzara
     * {@code practicedOn} con {@code createdDate}, con la misma fecha en las dos no
     * se veria.
     */
    private static final LocalDate PRACTICADA_EL = LocalDate.of(2026, 3, 5);
    private static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 7, 8, 45, 0);

    private static final int ANO = 2026;
    private static final BigDecimal BASE = new BigDecimal("1234567.89");

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DocumentWithholdingJpaRepository jpaRepository;

    private JpaDocumentWithholdingRepository repository;

    @BeforeEach
    void seed() {
        repository = new JpaDocumentWithholdingRepository(jpaRepository,
                new DocumentWithholdingJpaMapper());
        SchemaSeed.seed(entityManager);
        municipioConCodigoDane();
        factura(FACTURA_PROPIA, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID, "FAC-8400",
                PERIODO_INICIO, PERIODO_FIN);
        factura(FACTURA_AJENA, SchemaSeed.OTRA_COMPANY_ID, SchemaSeed.OTRA_SUBSCRIPTION_ID,
                "FAC-8401", PERIODO_INICIO, PERIODO_FIN);
        factura(FACTURA_PROPIA_ANTERIOR, SchemaSeed.COMPANY_ID, SchemaSeed.SUBSCRIPTION_ID,
                "FAC-8402", PERIODO_ANTERIOR_INICIO, PERIODO_ANTERIOR_FIN);
        certificado(CERTIFICADO_PROPIO, SchemaSeed.COMPANY_ID, "CERT-8410");
        certificado(CERTIFICADO_AJENO, SchemaSeed.OTRA_COMPANY_ID, "CERT-8411");
        entityManager.flush();
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda la retencion y la recupera con cada importe y cada fecha en su sitio")
        void guarda_la_retencion_y_la_recupera_campo_a_campo() {
            DocumentWithholding guardada = repository.save(ica(new BigDecimal("0.690000"),
                    new BigDecimal("8518.52"), MUNICIPIO, "2026-B02"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperada -> {
                        assertThat(recuperada.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(recuperada.getBillingDocumentId()).isEqualTo(FACTURA_PROPIA);
                        assertThat(recuperada.getType()).isEqualTo(WithholdingType.ICA);
                        assertThat(recuperada.getTaxableBase()).isEqualByComparingTo("1234567.89");
                        assertThat(recuperada.getAmount()).isEqualByComparingTo("8518.52");
                        assertThat(recuperada.getMunicipalityCode()).isEqualTo(MUNICIPIO);
                        assertThat(recuperada.getFiscalYear()).isEqualTo(ANO);
                        assertThat(recuperada.getFiscalPeriodKey()).isEqualTo("2026-B02");
                        assertThat(recuperada.getPracticedOn()).isEqualTo(PRACTICADA_EL);
                        assertThat(recuperada.getCreatedDate()).isEqualTo(CREADA_EL);
                        assertThat(recuperada.getCertificateId()).isNull();
                    });
        }

        @Test
        @DisplayName("la tarifa por mil sobrevive al viaje con sus seis decimales")
        void la_tarifa_por_mil_sobrevive_con_sus_seis_decimales() {
            // 4,14 por mil es 0,414 %. La columna es DECIMAL(9,6): si alguien la
            // redujera a cuatro decimales, o el mapper redondeara, base por tarifa
            // dejaria de dar el importe certificado y se retendria de menos en cada
            // factura, siempre en la misma direccion y sin una sola alarma.
            DocumentWithholding guardada = repository.save(ica(new BigDecimal("0.414000"),
                    new BigDecimal("5111.11"), MUNICIPIO, "2026-B03"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperada -> {
                        assertThat(recuperada.getRatePercent()).isEqualByComparingTo("0.414000");
                        assertThat(recuperada.getRatePercent()).isNotEqualByComparingTo("0.410000");
                    });
        }

        @Test
        @DisplayName("apuntarla a su certificado actualiza la fila y mueve la version")
        void apuntarla_a_su_certificado_mueve_la_version() {
            DocumentWithholding sinRespaldo = repository.save(renta("2026-A"));
            entityManager.flush();
            entityManager.clear();

            DocumentWithholding respaldada = repository
                    .save(sinRespaldo.linkTo(CERTIFICADO_PROPIO));
            entityManager.flush();
            entityManager.clear();

            // Es un UPDATE y no un INSERT: el id es el mismo y la version subio. Si el
            // mapper perdiera la version, Hibernate insertaria una fila nueva y la
            // original se quedaria sin certificado para siempre.
            assertThat(respaldada.getId()).isEqualTo(sinRespaldo.getId());
            assertThat(respaldada.getVersion()).isGreaterThan(sinRespaldo.getVersion());
            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).totalElements())
                    .isEqualTo(1L);
            assertThat(repository.findByIdAndCompanyId(respaldada.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperada -> assertThat(recuperada.getCertificateId())
                            .isEqualTo(CERTIFICADO_PROPIO));
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("dos retenciones NACIONALES del mismo documento y tipo chocan por el centinela")
        void dos_retenciones_nacionales_del_mismo_caso_chocan() {
            repository.save(renta("2026-A"));
            entityManager.flush();

            // ESTE es el caso que justifica municipality_key. Las dos filas tienen
            // municipality_code NULL, y en un indice unico de MySQL dos NULL no chocan
            // entre si: sin la columna generada con centinela '-', la segunda habria
            // entrado y la factura quedaria saldada dos veces por la misma retencion.
            EngineConstraint.assertViolates("uq_document_withholdings_case", () -> {
                repository.save(renta("2026-A"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos retenciones de ICA del mismo documento y municipio tambien chocan")
        void dos_retenciones_de_ica_del_mismo_municipio_chocan() {
            repository.save(ica(new BigDecimal("0.690000"), new BigDecimal("8518.52"), MUNICIPIO,
                    "2026-B02"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_document_withholdings_case", () -> {
                repository.save(ica(new BigDecimal("0.690000"), new BigDecimal("8518.52"),
                        MUNICIPIO, "2026-B02"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos tipos distintos sobre el mismo documento SI caben, que es lo normal")
        void dos_tipos_distintos_sobre_el_mismo_documento_si_caben() {
            // Una misma factura lleva con frecuencia retefuente y reteiva a la vez. Si
            // la unicidad no incluyera el tipo, registrar la segunda seria imposible.
            repository.save(renta("2026-A"));
            repository.save(iva("2026-B02"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20).totalElements())
                    .isEqualTo(2L);
        }

        @Test
        @DisplayName("un periodo que no corresponde al tipo lo para chk_..._period")
        void un_periodo_que_no_corresponde_al_tipo_lo_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de
            // comprobar que la base tambien la cuida —el cinturon bajo el tirante— es
            // escribir la fila por SQL nativo, saltandose el agregado. Con la
            // granularidad anual que tenia la ficha, este CHECK no podia existir.
            EngineConstraint.assertViolates("chk_document_withholdings_period",
                    () -> insertaCruda("INCOME_TAX", null, ANO, "2026-B03"));
        }

        @Test
        @DisplayName("un periodo de otro ano que el declarado lo para el mismo check")
        void un_periodo_de_otro_ano_lo_para_el_mismo_check() {
            // La otra mitad del CHECK: sin ella, fiscal_year 2026 con '2025-B02'
            // entraria y la retencion se declararia en un periodo de otro ejercicio.
            EngineConstraint.assertViolates("chk_document_withholdings_period",
                    () -> insertaCruda("VAT", null, ANO, "2025-B02"));
        }

        @Test
        @DisplayName("una retencion de ICA sin municipio la para chk_..._municipality")
        void una_retencion_de_ica_sin_municipio_la_para_el_check() {
            EngineConstraint.assertViolates("chk_document_withholdings_municipality",
                    () -> insertaCruda("ICA", null, ANO, "2026-B02"));
        }

        @Test
        @DisplayName("una retencion nacional CON municipio la para el mismo check")
        void una_retencion_nacional_con_municipio_la_para_el_mismo_check() {
            // Sin esta mitad, un CHECK que solo mirara la rama de ICA pasaria por bueno
            // y una retefuente podria afirmar un municipio que no le corresponde.
            EngineConstraint.assertViolates("chk_document_withholdings_municipality",
                    () -> insertaCruda("VAT", MUNICIPIO, ANO, "2026-B02"));
        }

        @Test
        @DisplayName("un municipio que no esta en cities lo para fk_..._municipality")
        void un_municipio_inexistente_lo_para_la_fk() {
            // '99999' no es ningun municipio DIVIPOLA sembrado. La FK contra
            // cities.dane_code es lo que impide una retencion de ICA imposible de
            // cruzar contra la tarifa de nadie —y es la que obligo al changeset 315 a
            // alinear antes la colacion de esa columna—.
            EngineConstraint.assertViolates("fk_document_withholdings_municipality",
                    () -> insertaCruda("ICA", "99999", ANO, "2026-B02"));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la carga por id no cruza de empresa")
        void la_carga_por_id_no_cruza_de_empresa() {
            DocumentWithholding guardada = repository.save(renta("2026-A"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(
                    repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("una retencion no cuelga de la factura de otra empresa: fk_..._document")
        void una_retencion_no_cuelga_de_la_factura_de_otra_empresa() {
            // FACTURA_AJENA existe de verdad, solo que bajo OTRA_COMPANY_ID. La empresa
            // y el resto de datos son validos, asi que la unica barandilla que puede
            // pararlo es la FK COMPUESTA (company_id, billing_document_id). Si manana
            // alguien la degradara a una FK simple, esta fila entraria y el caso se
            // pondria rojo.
            EngineConstraint.assertViolates("fk_document_withholdings_document", () -> {
                repository.save(new DocumentWithholding(null, SchemaSeed.COMPANY_ID, FACTURA_AJENA,
                        WithholdingType.INCOME_TAX, BASE, new BigDecimal("2.500000"),
                        new BigDecimal("30864.20"), null, ANO, "2026-A", PRACTICADA_EL, null,
                        CREADA_EL, null));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("una retencion no se respalda con el certificado de otra empresa")
        void una_retencion_no_se_respalda_con_el_certificado_de_otra_empresa() {
            DocumentWithholding propia = repository.save(renta("2026-A"));
            entityManager.flush();
            entityManager.clear();

            // Mismo criterio que la anterior sobre la otra FK compuesta,
            // (company_id, certificate_id). Es lo que hace que el ValidationPort del
            // servicio no sea decorativo: sin el, el operador veria este error de
            // integridad como un 500 sin explicacion.
            EngineConstraint.assertViolates("fk_document_withholdings_certificate", () -> {
                repository.save(propia.linkTo(CERTIFICADO_AJENO));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la bandeja de reclamacion de una empresa no ve la de la vecina")
        void la_bandeja_de_una_empresa_no_ve_la_de_la_vecina() {
            repository.save(renta("2026-A"));
            repository.save(rentaAjena("2026-A"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository
                    .findAllUncertifiedByCompanyIdAndFiscalYear(SchemaSeed.COMPANY_ID, ANO, 0, 20)
                    .content()).singleElement()
                    .satisfies(fila -> assertThat(fila.getBillingDocumentId())
                            .isEqualTo(FACTURA_PROPIA));
            // Y el barrido de plataforma si las ve las dos: es la diferencia entre los
            // dos casos de uso, y la razon de que sean dos puertos y no uno.
            assertThat(repository.findAllUncertifiedByFiscalYear(ANO, 0, 20).totalElements())
                    .isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("la vigilancia solo trae lo no certificado y del ano pedido")
        void la_vigilancia_solo_trae_lo_no_certificado_y_del_ano_pedido() {
            DocumentWithholding respaldada = repository.save(renta("2026-A"));
            repository.save(iva("2026-B02"));
            repository.save(rentaDelAno(2025, "2025-A"));
            entityManager.flush();
            repository.save(respaldada.linkTo(CERTIFICADO_PROPIO));
            entityManager.flush();
            entityManager.clear();

            // De las tres, solo el IVA de 2026 sigue sin papel. La de renta ya lo tiene
            // y la de 2025 es de otro ejercicio: mezclarlas daria una cifra a reclamar
            // que no corresponde a ninguna declaracion.
            assertThat(repository.findAllUncertifiedByFiscalYear(ANO, 0, 20).content())
                    .singleElement()
                    .satisfies(fila -> assertThat(fila.getType()).isEqualTo(WithholdingType.VAT));
            assertThat(repository.findAllUncertifiedByFiscalYear(2025, 0, 20).totalElements())
                    .isEqualTo(1L);
        }

        @Test
        @DisplayName("ordena por fecha de practica descendente y desempata por id")
        void ordena_por_fecha_descendente_y_desempata_por_id() {
            DocumentWithholding antigua = repository
                    .save(rentaPracticadaEl(LocalDate.of(2026, 1, 15), "2026-A"));
            // Las dos siguientes comparten fecha EXACTA, que es lo normal: una misma
            // factura lleva retefuente, reteiva y reteica el mismo dia. Sin el id de
            // desempate el orden entre ellas seria el que quiera el motor y la
            // paginacion podria repetir o perder una — y una bandeja de reclamacion que
            // pierde filas al paginar es peor que no tenerla.
            DocumentWithholding empateA = repository.save(iva("2026-B02"));
            DocumentWithholding empateB = repository.save(ica(new BigDecimal("0.690000"),
                    new BigDecimal("8518.52"), MUNICIPIO, "2026-B02"));
            entityManager.flush();
            entityManager.clear();

            PageResult<DocumentWithholding> pagina = repository
                    .findAllByCompanyId(SchemaSeed.COMPANY_ID, 0, 20);

            assertThat(pagina.content()).extracting(DocumentWithholding::getId)
                    .containsExactly(empateB.getId(), empateA.getId(), antigua.getId());
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }
    }

    // --- andamio ------------------------------------------------------------

    private DocumentWithholding renta(String periodo) {
        return retencion(SchemaSeed.COMPANY_ID, FACTURA_PROPIA, WithholdingType.INCOME_TAX,
                new BigDecimal("2.500000"), new BigDecimal("30864.20"), null, ANO, periodo,
                PRACTICADA_EL);
    }

    private DocumentWithholding rentaAjena(String periodo) {
        return retencion(SchemaSeed.OTRA_COMPANY_ID, FACTURA_AJENA, WithholdingType.INCOME_TAX,
                new BigDecimal("2.500000"), new BigDecimal("30864.20"), null, ANO, periodo,
                PRACTICADA_EL);
    }

    /**
     * Cuelga de {@link #FACTURA_PROPIA_ANTERIOR} y no de la del ano en curso: la
     * unicidad por caso no mira el ano, asi que dos retenciones de renta sobre el
     * mismo documento chocarian aunque fueran de ejercicios distintos.
     */
    private DocumentWithholding rentaDelAno(int ano, String periodo) {
        return retencion(SchemaSeed.COMPANY_ID, FACTURA_PROPIA_ANTERIOR, WithholdingType.INCOME_TAX,
                new BigDecimal("2.500000"), new BigDecimal("30864.20"), null, ano, periodo,
                LocalDate.of(ano, 3, 5));
    }

    private DocumentWithholding rentaPracticadaEl(LocalDate practicadaEl, String periodo) {
        return retencion(SchemaSeed.COMPANY_ID, FACTURA_PROPIA, WithholdingType.INCOME_TAX,
                new BigDecimal("2.500000"), new BigDecimal("30864.20"), null, ANO, periodo,
                practicadaEl);
    }

    private DocumentWithholding iva(String periodo) {
        return retencion(SchemaSeed.COMPANY_ID, FACTURA_PROPIA, WithholdingType.VAT,
                new BigDecimal("15.000000"), new BigDecimal("185185.18"), null, ANO, periodo,
                PRACTICADA_EL);
    }

    private DocumentWithholding ica(BigDecimal tarifa, BigDecimal retenido, String municipio,
            String periodo) {
        return retencion(SchemaSeed.COMPANY_ID, FACTURA_PROPIA, WithholdingType.ICA, tarifa,
                retenido, municipio, ANO, periodo, PRACTICADA_EL);
    }

    private DocumentWithholding retencion(Long companyId, Long facturaId, WithholdingType tipo,
            BigDecimal tarifa, BigDecimal retenido, String municipio, int ano, String periodo,
            LocalDate practicadaEl) {
        return new DocumentWithholding(null, companyId, facturaId, tipo, BASE, tarifa, retenido,
                municipio, ano, periodo, practicadaEl, null, CREADA_EL, null);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para los CHECK que el dominio
     * ya replica: sin ella no habria forma de comprobar que la base tambien los
     * cuida.
     */
    private void insertaCruda(String tipo, String municipio, int ano, String periodo) {
        entityManager.createNativeQuery("""
                INSERT INTO document_withholdings (company_id, billing_document_id,
                                                   withholding_type, taxable_base, rate_percent,
                                                   amount, municipality_code, fiscal_year,
                                                   fiscal_period_key, practiced_on, created_date,
                                                   version)
                VALUES (:companyId, :facturaId, :tipo, 1234567.89, 2.500000, 30864.20, :municipio,
                        :ano, :periodo, :practicadaEl, :creadaEl, 0)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("facturaId", FACTURA_PROPIA).setParameter("tipo", tipo)
                .setParameter("municipio", municipio).setParameter("ano", ano)
                .setParameter("periodo", periodo).setParameter("practicadaEl", PRACTICADA_EL)
                .setParameter("creadaEl", CREADA_EL).executeUpdate();
    }

    /**
     * El municipio del seed nace sin codigo DIVIPOLA, y la FK de esta tabla apunta
     * a {@code cities.dane_code} y no a {@code cities.id}. Se le pone aqui en vez
     * de sembrar una ciudad nueva porque {@code uq_cities_dane_code} es global: dos
     * rodajas sembrando el mismo codigo chocarian.
     *
     * <p>
     * Y por eso mismo el codigo que se escribe tiene que ser uno que ninguna
     * migracion siembre: ver {@link #MUNICIPIO}. El {@code UPDATE} choca igual
     * contra una fila de Liquibase que contra otra rodaja.
     */
    private void municipioConCodigoDane() {
        entityManager.createNativeQuery("""
                UPDATE cities SET dane_code = :codigo WHERE id = :id
                """).setParameter("codigo", MUNICIPIO).setParameter("id", SchemaSeed.CITY_ID)
                .executeUpdate();
    }

    private void factura(Long id, Long companyId, Long subscriptionId, String numero,
            String periodoInicio, String periodoFin) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_billing_documents (id, document_number, company_id,
                                                            subscription_id, document_kind,
                                                            billing_reason, period_start,
                                                            period_end, issue_status,
                                                            subtotal_amount, tax_amount,
                                                            total_amount, settled_amount,
                                                            created_date, version)
                VALUES (:id, :numero, :companyId, :subscriptionId, 'INVOICE', 'RECURRING_CYCLE',
                        :periodoInicio, :periodoFin, 'DRAFT', 1234567.89, 0.00, 1234567.89,
                        0.00, NOW(), 0)
                """).setParameter("id", id).setParameter("numero", numero)
                .setParameter("companyId", companyId).setParameter("subscriptionId", subscriptionId)
                .setParameter("periodoInicio", periodoInicio).setParameter("periodoFin", periodoFin)
                .executeUpdate();
    }

    /**
     * Certificado minimo. {@code received_on} y {@code file_ref} van los dos o
     * ninguno ({@code chk_withholding_certificates_file}); aqui ninguno, que es el
     * estado «expedido y aun no recibido».
     */
    private void certificado(Long id, Long companyId, String numero) {
        entityManager.createNativeQuery("""
                INSERT INTO withholding_certificates (id, company_id, issued_by_tax_id,
                                                      certificate_number, withholding_type,
                                                      fiscal_year, fiscal_period_key, rate_percent,
                                                      certified_amount, issued_on,
                                                      legal_deadline_on, created_date, version)
                VALUES (:id, :companyId, '900123456', :numero, 'INCOME_TAX', 2026, '2026-A',
                        2.500000, 30864.20, '2026-03-01', '2027-03-31', NOW(), 0)
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("numero", numero).executeUpdate();
    }
}
