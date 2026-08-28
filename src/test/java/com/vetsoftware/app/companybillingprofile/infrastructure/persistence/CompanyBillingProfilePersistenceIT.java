package com.vetsoftware.app.companybillingprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaCompanyBillingProfileRepository} contra MySQL real.
 *
 * <h2>Lo que solo se puede probar aqui</h2>
 *
 * <p>
 * <b>Que dos fichas vigentes de la misma empresa NO caben.</b> Esa invariante
 * no vive en ninguna linea de Java: la sostiene la columna generada
 * {@code current_profile_marker} —que vale la empresa mientras {@code valid_to}
 * es nulo y se vacia cuando deja de serlo— y el indice unico
 * {@code uq_company_billing_profiles_current} que hay encima. El servicio hace
 * una comprobacion previa, pero entre su lectura y el {@code INSERT} cabe otra
 * transaccion: lo unico que de verdad garantiza la unicidad es el motor.
 * {@link UnSoloPerfilVigente#dos_fichas_vigentes_de_la_misma_empresa_no_caben()}
 * lo congela.
 *
 * <p>
 * <b>Y que el orden de las dos escrituras de la sucesion funciona.</b>
 * Hibernate ejecuta todos los {@code INSERT} antes que los {@code UPDATE} de la
 * misma transaccion, asi que sin el flush que el puerto exige la sucesora
 * entraria mientras la anterior sigue vigente y chocaria contra ese mismo
 * indice. Un test de servicio con dobles pasa igual con el flush y sin el.
 *
 * <h2>Por que el adaptador se construye a mano</h2>
 *
 * <p>
 * {@code PersistenceSliceConfig} reune los adaptadores de las rodajas para que
 * todas compartan una unica clave de {@code MergedContextConfiguration} y, con
 * ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio con
 * este adaptador volveria a darle a esta clase una clave unica y un arranque de
 * contexto entero para ella sola. Instanciarlo con los dos repositorios de
 * Spring Data que la rodaja ya expone cuesta una linea y no ejercita menos SQL.
 *
 * <p>
 * <b>Los ids crudos van en el rango 8800</b>, que ninguna otra rodaja usa.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaCompanyBillingProfileRepository — la ficha de facturacion contra MySQL real")
class CompanyBillingProfilePersistenceIT extends AbstractDataJpaTest {

    private static final Long COMPANY = SchemaSeed.COMPANY_ID;
    private static final Long OTRA_COMPANY = SchemaSeed.OTRA_COMPANY_ID;

    private static final Long CRUDA = 8800L;

    private static final LocalDate RIGE_DESDE = CompanyBillingProfileMother.RIGE_DESDE;
    private static final LocalDate SUCEDE_DESDE = CompanyBillingProfileMother.SUCEDE_DESDE;

    private static final String NULO = "NULL";

    @Autowired
    private CompanyBillingProfileJpaRepository springDataRepository;
    @Autowired
    private CityJpaRepository cityJpaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaCompanyBillingProfileRepository repository;

    @BeforeEach
    void sembrarYConstruirElAdaptador() {
        SchemaSeed.seed(entityManager);
        entityManager.flush();

        // Guardia de la siembra: las dos empresas y el municipio son las claves
        // foraneas de esta tabla. Sin ellas el fallo saldria como una violacion de FK
        // varias lineas mas abajo, apuntando a la tabla equivocada.
        assertThat(filas("companies", COMPANY)).as("la empresa dueña de la ficha").isOne();
        assertThat(filas("companies", OTRA_COMPANY)).as("la empresa del otro tenant").isOne();
        assertThat(filas("cities", SchemaSeed.CITY_ID)).as("el municipio de la direccion").isOne();

        repository = new JpaCompanyBillingProfileRepository(springDataRepository, cityJpaRepository,
                new CompanyBillingProfileJpaMapper());
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la ficha de una sociedad y la recupera con cada campo en su sitio")
        void guarda_la_ficha_de_una_sociedad_y_la_recupera() {
            CompanyBillingProfile guardada = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).get()
                    .satisfies(ficha -> {
                        assertThat(ficha.getTaxId()).isEqualTo(CompanyBillingProfileMother.NIT);
                        assertThat(ficha.getVerificationDigit())
                                .isEqualTo(CompanyBillingProfileMother.DIGITO_VERIFICACION);
                        assertThat(ficha.getLegalName())
                                .isEqualTo(CompanyBillingProfileMother.RAZON_SOCIAL);
                        assertThat(ficha.getFirstName()).isNull();
                        assertThat(ficha.getAddress())
                                .isEqualTo(CompanyBillingProfileMother.DIRECCION);
                        // Se afirma el ID y no el nombre. SchemaSeed.insert() es un no-op
                        // cuando el id ya esta ocupado, y la ciudad 900 ya la ocupa la
                        // migracion 022_seed_americas_geography.sql: el literal 'Medellin'
                        // de la siembra de test NUNCA llega a escribirse. Lo que esta
                        // rodaja tiene que probar es que el @ManyToOne y el @EntityGraph
                        // traen la ciudad correcta, y eso lo prueba el id.
                        assertThat(ficha.getCity().id()).isEqualTo(SchemaSeed.CITY_ID);
                        assertThat(ficha.getCity().name()).isNotBlank();
                        assertThat(ficha.getBillingEmail())
                                .isEqualTo(CompanyBillingProfileMother.CORREO);
                        assertThat(ficha.isWithholdingAgent()).isTrue();
                        assertThat(ficha.getValidFrom()).isEqualTo(RIGE_DESDE);
                        assertThat(ficha.getValidTo()).isNull();
                        assertThat(ficha.getVersion()).isZero();
                    });
        }

        @Test
        @DisplayName("los cuatro campos de nombre de la persona natural sobreviven al viaje separados")
        void los_cuatro_campos_de_nombre_sobreviven_separados() {
            CompanyBillingProfile guardada = repository
                    .save(CompanyBillingProfileMother.personaNatural());
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), COMPANY)).get()
                    .satisfies(ficha -> {
                        assertThat(ficha.getFirstName())
                                .isEqualTo(CompanyBillingProfileMother.PRIMER_NOMBRE);
                        assertThat(ficha.getMiddleName())
                                .isEqualTo(CompanyBillingProfileMother.OTROS_NOMBRES);
                        assertThat(ficha.getLastName())
                                .isEqualTo(CompanyBillingProfileMother.PRIMER_APELLIDO);
                        assertThat(ficha.getSecondLastName())
                                .isEqualTo(CompanyBillingProfileMother.SEGUNDO_APELLIDO);
                        assertThat(ficha.getLegalName()).isNull();
                    });
        }

        @Test
        @DisplayName("los tres enums viajan como texto a sus columnas VARCHAR")
        void los_tres_enums_viajan_como_texto() {
            // Si alguien cambiara el @Enumerated a ORDINAL, los CHECK de la tabla
            // rechazarian el INSERT: esta asercion lo dice antes y con nombre.
            CompanyBillingProfile guardada = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            entityManager.flush();

            assertThat(textoDe("person_kind", guardada.getId())).isEqualTo("LEGAL");
            assertThat(textoDe("tax_id_kind", guardada.getId())).isEqualTo("NIT");
            assertThat(textoDe("tax_regime", guardada.getId())).isEqualTo("COMMON");
        }
    }

    @Nested
    @DisplayName("Un solo perfil vigente")
    class UnSoloPerfilVigente {

        @Test
        @DisplayName("dos fichas vigentes de la misma empresa NO caben: uq_..._current")
        void dos_fichas_vigentes_de_la_misma_empresa_no_caben() {
            // LA invariante de la feature, y la unica que no vive en Java. Las dos fechas
            // de inicio son distintas a proposito: con la misma saltaria antes
            // uq_company_billing_profiles_validity y este caso seguiria verde el dia que
            // alguien borrara la columna generada.
            repository.save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));

            EngineConstraint.assertViolates("uq_company_billing_profiles_current", () -> {
                repository.save(CompanyBillingProfileMother.sociedadDe(COMPANY, SUCEDE_DESDE));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("dos fichas vigentes de empresas DISTINTAS si caben: el marcador es la empresa")
        void dos_fichas_vigentes_de_empresas_distintas_si_caben() {
            repository.save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            repository.save(CompanyBillingProfileMother.sociedadDe(OTRA_COMPANY, RIGE_DESDE));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findCurrentByCompanyId(COMPANY)).isPresent();
            assertThat(repository.findCurrentByCompanyId(OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("una cerrada y una vigente de la misma empresa si caben: dos marcadores vacios no chocan")
        void una_cerrada_y_una_vigente_si_caben() {
            // Es lo que hace que la historia quepa entera. Si el indice mirara company_id
            // en vez del marcador, la segunda ficha de una empresa seria imposible y el
            // NIT no se podria cambiar nunca.
            CompanyBillingProfile vigente = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            vigente.closeOn(SUCEDE_DESDE);
            repository.save(vigente);
            repository.save(CompanyBillingProfileMother.sociedadDe(COMPANY, SUCEDE_DESDE));
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(COMPANY, 0, 20).totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("dos fichas de la misma empresa con la MISMA fecha de inicio no caben: uq_..._validity")
        void dos_fichas_con_la_misma_fecha_de_inicio_no_caben() {
            // La segunda unicidad de la tabla, y la que impide que dos sucesiones el
            // mismo dia dejen la historia sin saber cual rige.
            CompanyBillingProfile vigente = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            vigente.closeOn(SUCEDE_DESDE);
            repository.save(vigente);

            EngineConstraint.assertViolates("uq_company_billing_profiles_validity", () -> {
                insertarCruda(CRUDA, COMPANY, "LEGAL", "NIT", "900999888",
                        texto("Otra Sociedad SAS"), NULO, NULO, "facturacion@otra.com", "SPECIAL",
                        "2026-01-15", NULO);
                entityManager.flush();
            });
        }
    }

    @Nested
    @DisplayName("Sucesion")
    class Sucesion {

        @Test
        @DisplayName("el ciclo completo: la vigente se cierra, la sucesora nace, y las dos quedan")
        void el_ciclo_completo_deja_las_dos_fichas() {
            CompanyBillingProfile vigente = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));

            vigente.closeOn(SUCEDE_DESDE);
            repository.save(vigente);
            CompanyBillingProfile sucesora = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, SUCEDE_DESDE));
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(vigente.getId(), COMPANY)).get()
                    .satisfies(cerrada -> {
                        assertThat(cerrada.getValidTo()).isEqualTo(SUCEDE_DESDE);
                        assertThat(cerrada.isCurrent()).isFalse();
                        // El UPDATE paso por el ciclo de Hibernate y no por una escritura
                        // masiva que dejaria la version intacta.
                        assertThat(cerrada.getVersion()).isEqualTo(1L);
                    });
            assertThat(repository.findCurrentByCompanyId(COMPANY)).get()
                    .satisfies(actual -> assertThat(actual.getId()).isEqualTo(sucesora.getId()));
        }

        @Test
        @DisplayName("guardar la cerrada ANTES que la sucesora funciona porque el puerto vacia el buffer")
        void guardar_la_cerrada_antes_que_la_sucesora_funciona() {
            // Este caso es la red del flush. Con un save() sin flush, Hibernate mandaria
            // el INSERT de la sucesora antes que el UPDATE de la cerrada, las dos filas
            // calcularian el mismo current_profile_marker y esto moriria con un
            // Duplicate entry sobre una columna que nadie escribio.
            CompanyBillingProfile vigente = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            vigente.closeOn(SUCEDE_DESDE);

            repository.save(vigente);
            CompanyBillingProfile sucesora = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, SUCEDE_DESDE));

            assertThat(sucesora.getId()).isNotNull().isNotEqualTo(vigente.getId());
        }

        @Test
        @DisplayName("la ficha cerrada deja de ser la vigente aunque siga en la tabla")
        void la_ficha_cerrada_deja_de_ser_la_vigente() {
            CompanyBillingProfile vigente = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            vigente.closeOn(SUCEDE_DESDE);
            repository.save(vigente);
            entityManager.clear();

            assertThat(repository.findCurrentByCompanyId(COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(vigente.getId(), COMPANY)).isPresent();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("una sociedad SIN razon social la para chk_..._name_shape")
        void una_sociedad_sin_razon_social_la_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de comprobar
            // que la base tambien la cuida —el cinturon bajo el tirante— es escribir la
            // fila por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_company_billing_profiles_name_shape",
                    () -> insertarCruda(CRUDA, COMPANY, "LEGAL", "NIT", "900111222", NULO, NULO,
                            NULO, "facturacion@otra.com", "COMMON", "2026-02-01", NULO));
        }

        @Test
        @DisplayName("una sociedad CON apellidos la para el mismo check")
        void una_sociedad_con_apellidos_la_para_el_mismo_check() {
            // La otra mitad de la rama. Sin este caso, una ficha ambigua sobre que juego
            // de columnas hay que reportar a la administracion pasaria por buena.
            EngineConstraint.assertViolates("chk_company_billing_profiles_name_shape",
                    () -> insertarCruda(CRUDA + 1, COMPANY, "LEGAL", "NIT", "900111333",
                            texto("Inversiones Pet SAS"), texto("Ana"), texto("Ruiz"),
                            "facturacion@otra.com", "COMMON", "2026-02-02", NULO));
        }

        @Test
        @DisplayName("una persona natural SIN primer apellido la para el mismo check")
        void una_persona_natural_sin_apellido_la_para_el_mismo_check() {
            EngineConstraint.assertViolates("chk_company_billing_profiles_name_shape",
                    () -> insertarCruda(CRUDA + 2, COMPANY, "NATURAL", "CC", "43215678", NULO,
                            texto("Ana"), NULO, "ana@correo.com", "SIMPLE", "2026-02-03", NULO));
        }

        @Test
        @DisplayName("una vigencia que termina el mismo dia en que empieza la para chk_..._validity")
        void una_vigencia_de_duracion_cero_la_para_el_check() {
            // El CHECK es estricto (>), no >=. Esta es la comprobacion que hace que la
            // sucesion en el mismo dia no sea representable, y por eso el dominio la
            // rechaza en vez de correr la fecha en silencio.
            EngineConstraint.assertViolates("chk_company_billing_profiles_validity",
                    () -> insertarCruda(CRUDA + 3, COMPANY, "LEGAL", "NIT", "900111444",
                            texto("Inversiones Pet SAS"), NULO, NULO, "facturacion@otra.com",
                            "COMMON", "2026-02-04", texto("2026-02-04")));
        }

        @Test
        @DisplayName("un correo sin arroba lo para chk_..._email")
        void un_correo_sin_arroba_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_company_billing_profiles_email",
                    () -> insertarCruda(CRUDA + 4, COMPANY, "LEGAL", "NIT", "900111555",
                            texto("Inversiones Pet SAS"), NULO, NULO, "facturacion.otra.com",
                            "COMMON", "2026-02-05", NULO));
        }

        @Test
        @DisplayName("un tipo de persona fuera de los dos lo para el motor, via chk_..._name_shape")
        void un_tipo_de_persona_desconocido_lo_para_el_check() {
            // La red del enum sigue ahi: si alguien anade un valor a PersonKind sin el
            // changeset que lo admita, el INSERT muere.
            //
            // PERO NO LO MATA LA CONSTRAINT QUE PARECE. chk_..._person_kind no llega a
            // evaluarse para un valor desconocido, porque chk_..._name_shape ya lo
            // excluye: sus dos ramas empiezan por person_kind = 'LEGAL' y
            // person_kind = 'NATURAL', asi que un 'EMPRESA' no cae en ninguna, tenga la
            // razon social puesta o los nombres partidos. El vocabulario esta vigilado
            // dos veces y la que dispara es la de la forma del nombre.
            //
            // Nombrar la otra era el defecto que EngineConstraint existe para cazar: el
            // caso habria seguido en verde el dia que alguien borrara del esquema
            // justamente la constraint que dice comprobar.
            EngineConstraint.assertViolates("chk_company_billing_profiles_name_shape",
                    () -> insertarCruda(CRUDA + 5, COMPANY, "EMPRESA", "NIT", "900111666",
                            texto("Inversiones Pet SAS"), NULO, NULO, "facturacion@otra.com",
                            "COMMON", "2026-02-06", NULO));
        }

        @Test
        @DisplayName("un regimen fuera de los cuatro lo para chk_..._tax_regime")
        void un_regimen_desconocido_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_company_billing_profiles_tax_regime",
                    () -> insertarCruda(CRUDA + 6, COMPANY, "LEGAL", "NIT", "900111777",
                            texto("Inversiones Pet SAS"), NULO, NULO, "facturacion@otra.com",
                            "REGIMEN_RARO", "2026-02-07", NULO));
        }

        @Test
        @DisplayName("una empresa que no existe la para la clave foranea a companies")
        void una_empresa_inexistente_la_para_la_clave_foranea() {
            EngineConstraint.assertViolates("fk_company_billing_profiles_company",
                    () -> insertarCruda(CRUDA + 7, 987654L, "LEGAL", "NIT", "900111888",
                            texto("Inversiones Pet SAS"), NULO, NULO, "facturacion@otra.com",
                            "COMMON", "2026-02-08", NULO));
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la ficha de otra empresa NO se puede cargar por id")
        void la_ficha_de_otra_empresa_no_se_puede_cargar_por_id() {
            // El id lo escribe el cliente en la URL. Sin el companyId en el WHERE,
            // cualquier empleado autenticado leeria el NIT, la direccion y el correo de
            // facturacion de otra clinica.
            CompanyBillingProfile ajena = repository
                    .save(CompanyBillingProfileMother.sociedadDe(OTRA_COMPANY, RIGE_DESDE));
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(ajena.getId(), COMPANY)).isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajena.getId(), OTRA_COMPANY)).isPresent();
        }

        @Test
        @DisplayName("el historico solo trae las fichas de la empresa que se pide")
        void el_historico_solo_trae_las_fichas_de_la_empresa() {
            repository.save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            repository.save(CompanyBillingProfileMother.sociedadDe(OTRA_COMPANY, RIGE_DESDE));
            entityManager.clear();

            PageResult<CompanyBillingProfile> propias = repository.findAllByCompanyId(COMPANY, 0,
                    20);

            assertThat(propias.totalElements()).isEqualTo(1L);
            assertThat(propias.content()).extracting(CompanyBillingProfile::getCompanyId)
                    .containsOnly(COMPANY);
        }

        @Test
        @DisplayName("la ficha vigente se busca acotada: la de otra empresa no la tapa")
        void la_ficha_vigente_se_busca_acotada() {
            repository.save(CompanyBillingProfileMother.sociedadDe(OTRA_COMPANY, RIGE_DESDE));
            entityManager.clear();

            assertThat(repository.findCurrentByCompanyId(COMPANY)).isEmpty();
        }
    }

    @Nested
    @DisplayName("El historico")
    class ElHistorico {

        @Test
        @DisplayName("ordena de la mas reciente a la mas antigua: la vigente primero")
        void ordena_de_la_mas_reciente_a_la_mas_antigua() {
            CompanyBillingProfile primera = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            primera.closeOn(SUCEDE_DESDE);
            repository.save(primera);
            CompanyBillingProfile segunda = repository
                    .save(CompanyBillingProfileMother.sociedadDe(COMPANY, SUCEDE_DESDE));
            entityManager.clear();

            assertThat(repository.findAllByCompanyId(COMPANY, 0, 20).content())
                    .extracting(CompanyBillingProfile::getId)
                    .containsExactly(segunda.getId(), primera.getId());
        }

        @Test
        @DisplayName("la pagina respeta el tope del kernel de paginacion")
        void la_pagina_respeta_el_tope_del_kernel() {
            repository.save(CompanyBillingProfileMother.sociedadDe(COMPANY, RIGE_DESDE));
            entityManager.clear();

            // 100000 no llega a la consulta: Pages.request lo acota a MAX_SIZE.
            assertThat(repository.findAllByCompanyId(COMPANY, 0, 100000).pageSize()).isEqualTo(200);
        }

        @Test
        @DisplayName("una empresa sin fichas devuelve una pagina vacia, no un error")
        void una_empresa_sin_fichas_devuelve_pagina_vacia() {
            assertThat(repository.findAllByCompanyId(COMPANY, 0, 20).content()).isEmpty();
        }
    }

    private long filas(String tabla, Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM " + tabla + " WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    private String textoDe(String columna, Long id) {
        return (String) entityManager
                .createNativeQuery(
                        "SELECT " + columna + " FROM company_billing_profiles WHERE id = :id")
                .setParameter("id", id).getSingleResult();
    }

    private static String texto(String valor) {
        return "'" + valor + "'";
    }

    /**
     * Escritura cruda que se salta el agregado, para los {@code CHECK} y las
     * unicidades que el dominio ya replica: sin ella no habria forma de comprobar
     * que la base tambien los cuida.
     *
     * <p>
     * <b>Las columnas opcionales van como literales SQL y no como parametros</b>
     * —de ahi el {@code NULL} y el {@code texto(...)}—: una consulta nativa sin
     * metadatos de tipo no puede inferir el tipo de un {@code null}, y el fallo
     * saldria como un error de binding que no tiene nada que ver con lo que el caso
     * quiere probar.
     *
     * <p>
     * <b>{@code current_profile_marker} no se nombra.</b> Es
     * {@code GENERATED ALWAYS} y MySQL devuelve el error 3105 si aparece en el
     * {@code INSERT}, aunque el valor sea {@code NULL}.
     */
    private void insertarCruda(Long id, Long companyId, String personKind, String taxIdKind,
            String taxId, String legalName, String firstName, String lastName, String billingEmail,
            String taxRegime, String validFrom, String validTo) {
        entityManager.createNativeQuery("""
                INSERT INTO company_billing_profiles
                    (id, company_id, person_kind, tax_id_kind, tax_id, verification_digit,
                     legal_name, first_name, middle_name, last_name, second_last_name,
                     address, city_id, billing_email, tax_regime, withholding_agent,
                     valid_from, valid_to, created_date, enabled, version)
                VALUES (%d, %d, '%s', '%s', '%s', NULL,
                        %s, %s, NULL, %s, NULL,
                        'Escritura cruda de prueba', %d, '%s', '%s', false,
                        '%s', %s, '2026-01-12 09:30:15', true, 0)
                """.formatted(id, companyId, personKind, taxIdKind, taxId, legalName, firstName,
                lastName, SchemaSeed.CITY_ID, billingEmail, taxRegime, validFrom, validTo))
                .executeUpdate();
        entityManager.flush();
    }
}
