package com.vetsoftware.app.platformtaxprofile.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaRepository;
import com.vetsoftware.app.platformtaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
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
 * Rodaja de {@code JpaPlatformTaxProfileRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es que no puedan coexistir dos
 * identidades fiscales vigentes.</b> Es la invariante central del changeset 367
 * y no se puede comprobar sin el motor: la sostiene
 * {@code uq_platform_tax_profiles_current} sobre la columna generada
 * {@code current_profile_marker}, que vale {@code 1} mientras {@code valid_to}
 * es nulo y {@code NULL} en cuanto se cierra. Ni el dominio ni el caso de uso
 * pueden imponerla —dos peticiones concurrentes leerian las dos que no hay
 * ninguna vigente— y su ausencia significaria que dos facturas emitidas el
 * mismo dia llevan razones sociales distintas.
 *
 * <p>
 * <b>Lo segundo que congela es el orden de las sentencias en la sucesion.</b>
 * La cola de acciones de Hibernate ejecuta <em>todos</em> los {@code INSERT}
 * antes que los {@code UPDATE}, asi que con un {@code save} normal la sucesora
 * entraria mientras la anterior sigue abierta, las dos calcularian el mismo
 * marcador y la unicidad pararia la operacion. El adaptador usa
 * {@code saveAndFlush} por eso, y
 * {@link Sucesion#la_sucesion_deja_dos_filas_y_la_vieja_conserva_su_razon_social()}
 * es lo que lo mantiene: el caso de uso se lee perfecto —cierra primero, abre
 * despues— y el defecto estaria debajo, en el orden que decide el framework.
 *
 * <p>
 * <b>La actividad economica se siembra aqui y no en {@code SchemaSeed}</b>, que
 * no la cubre. El codigo CIIU es sintetico a proposito: {@code code} es
 * {@code UNIQUE} global en {@code economic_activities} y un codigo real podria
 * chocar con la siembra de otra rodaja, produciendo un fallo que no habla de
 * esta feature. Por la misma razon esta clase <b>no siembra geografia</b>: no
 * la necesita, {@code platform_tax_profiles} no tiene ninguna clave foranea a
 * {@code cities}.
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores de las rodajas para que
 * todas compartan una unica clave de {@code MergedContextConfiguration} y, con
 * ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio con
 * este adaptador volveria a darle a esta clase una clave unica y un arranque de
 * contexto entero para ella sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPlatformTaxProfileRepository — la identidad fiscal de la plataforma contra MySQL real")
class PlatformTaxProfilePersistenceIT extends AbstractDataJpaTest {

    /** Codigo CIIU sintetico: {@code economic_activities.code} es UNIQUE global. */
    private static final String CIIU_CODE = "Z0001";

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate RELEVO = LocalDate.of(2027, 1, 1);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 1, 1, 8, 30, 0);

    private static final String RAZON_SOCIAL_VIEJA = "VetSoftware S.A.S.";
    private static final String RAZON_SOCIAL_NUEVA = "VetSoftware Colombia S.A.S.";

    @Autowired
    private PlatformTaxProfileJpaRepository springDataRepository;
    @Autowired
    private EconomicActivityJpaRepository economicActivityJpaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaPlatformTaxProfileRepository repository;
    private Long ciiuId;

    @BeforeEach
    void adaptador() {
        repository = new JpaPlatformTaxProfileRepository(springDataRepository,
                economicActivityJpaRepository, new PlatformTaxProfileJpaMapper());
        ciiuId = sembrarActividadEconomica();
    }

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("guarda la identidad y la recupera campo a campo, con su actividad economica")
        void guarda_la_identidad_y_la_recupera_campo_a_campo() {
            PlatformTaxProfile guardada = repository.save(identidad(RAZON_SOCIAL_VIEJA, DESDE));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get().satisfies(recuperada -> {
                assertThat(recuperada.getDocumentType()).isEqualTo(PlatformDocumentType.NIT);
                assertThat(recuperada.getDocumentId()).isEqualTo("900123456");
                assertThat(recuperada.getVerificationDigit()).isEqualTo("7");
                assertThat(recuperada.getLegalName()).isEqualTo(RAZON_SOCIAL_VIEJA);
                assertThat(recuperada.getTaxRegime()).isEqualTo(PlatformTaxRegime.RESPONSABLE_IVA);
                assertThat(recuperada.getFiscalEmail()).isEqualTo("facturacion@vetsoftware.co");
                assertThat(recuperada.getCommercialName()).isEqualTo("VetSoftware");
                assertThat(recuperada.getValidFrom()).isEqualTo(DESDE);
                assertThat(recuperada.getValidTo()).isNull();
                assertThat(recuperada.isCurrent()).isTrue();
                assertThat(recuperada.getCreatedDate()).isEqualTo(CREADO_EL);
                assertThat(recuperada.getVersion()).isZero();
                // El @EntityGraph la hidrato: sin el, esto seria un N+1 por fila.
                assertThat(recuperada.getEconomicActivity())
                        .isEqualTo(new EconomicActivityRef(ciiuId, CIIU_CODE, "Software"));
            });
        }

        @Test
        @DisplayName("la actividad economica es opcional: la columna es nulable y null sobrevive")
        void la_actividad_economica_es_opcional() {
            PlatformTaxProfile guardada = repository
                    .save(identidad(RAZON_SOCIAL_VIEJA, DESDE, null, true));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).get()
                    .satisfies(recuperada -> assertThat(recuperada.getEconomicActivity()).isNull());
        }

        @Test
        @DisplayName("selfWithholder viaja en TINYINT pelado y sobrevive en true y en false")
        void self_withholder_sobrevive_en_los_dos_valores() {
            // No es relleno: un columnDefinition = "TINYINT(1)" haria que el driver
            // reportara la columna como Types.BIT y ddl-auto: validate tumbaria el
            // arranque de la aplicacion entera. Que esta clase llegue a ejecutarse
            // ya lo prueba a medias; los dos valores cierran el otro medio, porque
            // un mapeo roto puede leer siempre false sin dar un solo error.
            PlatformTaxProfile autorretenedora = repository
                    .save(identidad(RAZON_SOCIAL_VIEJA, DESDE, ciiuId, true));
            entityManager.flush();
            entityManager.clear();
            assertThat(repository.findById(autorretenedora.getId())).get()
                    .satisfies(leida -> assertThat(leida.isSelfWithholder()).isTrue());

            PlatformTaxProfile cargada = repository.findById(autorretenedora.getId()).orElseThrow();
            cargada.closeOn(RELEVO);
            repository.save(cargada);
            PlatformTaxProfile noAutorretenedora = repository
                    .save(identidad(RAZON_SOCIAL_NUEVA, RELEVO, ciiuId, false));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(noAutorretenedora.getId())).get()
                    .satisfies(leida -> assertThat(leida.isSelfWithholder()).isFalse());
        }

        @Test
        @DisplayName("dos identidades no pueden abrir el mismo dia: uq_platform_tax_profiles_validity")
        void dos_identidades_no_pueden_abrir_el_mismo_dia() {
            // Se escribe la segunda por SQL nativo y YA CERRADA. Con valid_to escrito
            // su current_profile_marker vale NULL, asi que uq_..._current no puede
            // saltar antes y la unica barandilla que queda es la de la fecha de
            // apertura — que es la que este caso dice estar probando.
            repository.save(identidad(RAZON_SOCIAL_VIEJA, DESDE));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_platform_tax_profiles_validity",
                    () -> insertarCruda(RAZON_SOCIAL_NUEVA, DESDE, RELEVO));
        }
    }

    @Nested
    @DisplayName("Una sola vigente")
    class UnaSolaVigente {

        @Test
        @DisplayName("abrir una segunda con la primera abierta lo para uq_platform_tax_profiles_current")
        void dos_vigentes_a_la_vez_las_para_el_motor() {
            // ESTA ES LA INVARIANTE CENTRAL DE LA TABLA. Sin ella, dos facturas
            // emitidas el mismo dia pueden llevar razones sociales distintas, y no
            // hay forma de saber despues cual era la correcta.
            repository.save(identidad(RAZON_SOCIAL_VIEJA, DESDE));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_platform_tax_profiles_current", () -> {
                repository.save(identidad(RAZON_SOCIAL_NUEVA, RELEVO));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("cerrar la vigente libera el hueco y entonces si entra la sucesora")
        void cerrar_la_vigente_libera_el_hueco() {
            // La otra mitad. Sin este caso, una unicidad mal escrita —sobre una
            // constante en vez de sobre la columna generada— pasaria el caso de
            // arriba y dejaria la tabla sin poder cambiar nunca de identidad.
            PlatformTaxProfile primera = repository.save(identidad(RAZON_SOCIAL_VIEJA, DESDE));
            entityManager.flush();
            entityManager.clear();

            PlatformTaxProfile cargada = repository.findById(primera.getId()).orElseThrow();
            cargada.closeOn(RELEVO);
            repository.save(cargada);

            PlatformTaxProfile sucesora = repository.save(identidad(RAZON_SOCIAL_NUEVA, RELEVO));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findCurrent()).get().satisfies(vigente -> {
                assertThat(vigente.getId()).isEqualTo(sucesora.getId());
                assertThat(vigente.getLegalName()).isEqualTo(RAZON_SOCIAL_NUEVA);
            });
        }

        @Test
        @DisplayName("sin ninguna fila, findCurrent no encuentra nada — que es el estado real de hoy")
        void sin_ninguna_fila_no_hay_vigente() {
            // La tabla nace vacia a proposito: el changeset 367 no la sembro porque
            // no habia razon social ni NIT reales y no se inventaron. Este vacio es
            // lo que FindCurrentPlatformTaxProfileService convierte en
            // NoCurrentPlatformTaxProfileException, y de ahi sale el 503.
            assertThat(repository.findCurrent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Sucesion")
    class Sucesion {

        @Test
        @DisplayName("deja DOS filas y la vieja conserva su razon social original")
        void la_sucesion_deja_dos_filas_y_la_vieja_conserva_su_razon_social() {
            // El punto entero del diseño: la ficha no se edita, se sucede. Si esto
            // fuera un UPDATE en sitio, las facturas de hace dos años pasarian a
            // decir la razon social nueva y no habria forma de reconstruir con cual
            // se emitieron.
            PlatformTaxProfile primera = repository.save(identidad(RAZON_SOCIAL_VIEJA, DESDE));
            entityManager.flush();
            entityManager.clear();

            PlatformTaxProfile cargada = repository.findById(primera.getId()).orElseThrow();
            cargada.closeOn(RELEVO);
            repository.save(cargada);
            repository.save(identidad(RAZON_SOCIAL_NUEVA, RELEVO));
            entityManager.flush();
            entityManager.clear();

            PageResult<PlatformTaxProfile> historico = repository.findAll(0, 20);
            assertThat(historico.totalElements()).isEqualTo(2);
            // Orden del adaptador: la vigente primero y hacia atras.
            assertThat(historico.content()).extracting(PlatformTaxProfile::getLegalName)
                    .containsExactly(RAZON_SOCIAL_NUEVA, RAZON_SOCIAL_VIEJA);

            assertThat(repository.findById(primera.getId())).get().satisfies(vieja -> {
                assertThat(vieja.getLegalName()).isEqualTo(RAZON_SOCIAL_VIEJA);
                assertThat(vieja.getValidTo()).isEqualTo(RELEVO);
                assertThat(vieja.isCurrent()).isFalse();
                // El UPDATE paso por el ciclo de Hibernate, no por un @Query masivo.
                assertThat(vieja.getVersion()).isEqualTo(1L);
            });
        }

        @Test
        @DisplayName("el relevo no deja hueco ni solape: el intervalo es semiabierto")
        void el_relevo_no_deja_hueco_ni_solape() {
            // El dia del relevo lo cubre la sucesora y NO la anterior. Un >= en
            // isEffectiveOn haria que las dos aplicaran ese dia, y una factura
            // emitida entonces no sabria que razon social imprimir.
            PlatformTaxProfile primera = repository.save(identidad(RAZON_SOCIAL_VIEJA, DESDE));
            entityManager.flush();
            entityManager.clear();

            PlatformTaxProfile cargada = repository.findById(primera.getId()).orElseThrow();
            cargada.closeOn(RELEVO);
            repository.save(cargada);
            PlatformTaxProfile sucesora = repository.save(identidad(RAZON_SOCIAL_NUEVA, RELEVO));
            entityManager.flush();
            entityManager.clear();

            PlatformTaxProfile vieja = repository.findById(primera.getId()).orElseThrow();
            PlatformTaxProfile nueva = repository.findById(sucesora.getId()).orElseThrow();

            assertThat(vieja.isEffectiveOn(RELEVO.minusDays(1))).isTrue();
            assertThat(vieja.isEffectiveOn(RELEVO)).isFalse();
            assertThat(nueva.isEffectiveOn(RELEVO)).isTrue();
        }
    }

    @Nested
    @DisplayName("Vigencia")
    class Vigencia {

        @Test
        @DisplayName("una vigencia que cierra antes de abrir la para chk_platform_tax_profiles_validity")
        void una_vigencia_invertida_la_para_el_check() {
            // El dominio ya lo rechaza en validateValidity; comprobar que la base
            // tambien lo hace —el cinturon bajo el tirante— exige escribir la fila
            // por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_platform_tax_profiles_validity",
                    () -> insertarCruda(RAZON_SOCIAL_VIEJA, RELEVO, DESDE));
        }

        @Test
        @DisplayName("una vigencia de duracion cero tambien: el CHECK es estricto")
        void una_vigencia_de_duracion_cero_la_para_el_check() {
            // Es la mitad que separa > de >=, y de ella depende que una identidad
            // abierta hoy no se pueda suceder hoy —el corolario incomodo que
            // documenta PlatformTaxProfileSuccessionNotAfterCurrentException—.
            EngineConstraint.assertViolates("chk_platform_tax_profiles_validity",
                    () -> insertarCruda(RAZON_SOCIAL_VIEJA, DESDE, DESDE));
        }
    }

    @Nested
    @DisplayName("La columna generada")
    class ColumnaGenerada {

        @Test
        @DisplayName("current_profile_marker la calcula el motor: escribirla se rechaza con el 3105")
        void escribir_el_marcador_lo_rechaza_el_motor() {
            // Por que importa: si alguien mapeara la columna en la entidad JPA,
            // Hibernate la incluiria en TODOS los INSERT y ninguna identidad se
            // podria registrar. La entidad no la mapea justamente por esto, y este
            // caso congela el motivo.
            EngineConstraint.assertViolates("current_profile_marker",
                    () -> entityManager.createNativeQuery("""
                            INSERT INTO platform_tax_profiles (document_type, document_id,
                                    legal_name, tax_regime, fiscal_email, is_self_withholder,
                                    valid_from, current_profile_marker, created_date, version)
                            VALUES ('NIT', '900999999', 'Imposible', 'RESPONSABLE_IVA',
                                    'x@y.co', 1, '2028-01-01', 1, NOW(6), 0)
                            """).executeUpdate());
        }
    }

    // ── andamio ──────────────────────────────────────────────────────────────

    private PlatformTaxProfile identidad(String razonSocial, LocalDate desde) {
        return identidad(razonSocial, desde, ciiuId, true);
    }

    private PlatformTaxProfile identidad(String razonSocial, LocalDate desde, Long actividadId,
            boolean autorretenedor) {
        EconomicActivityRef actividad = actividadId == null
                ? null
                : new EconomicActivityRef(actividadId, CIIU_CODE, "Software");
        return PlatformTaxProfile.open(PlatformDocumentType.NIT, "900123456", "7", razonSocial,
                PlatformTaxRegime.RESPONSABLE_IVA, "facturacion@vetsoftware.co", "VetSoftware",
                actividad, autorretenedor, desde, CREADO_EL);
    }

    /**
     * Escribe la fila por SQL nativo, saltandose el agregado y sus invariantes. Es
     * la unica forma de comprobar que la barandilla del motor existe de verdad y no
     * solo la del dominio.
     */
    private void insertarCruda(String razonSocial, LocalDate desde, LocalDate hasta) {
        entityManager.createNativeQuery("""
                INSERT INTO platform_tax_profiles (document_type, document_id, verification_digit,
                        legal_name, tax_regime, fiscal_email, is_self_withholder,
                        valid_from, valid_to, created_date, version)
                VALUES ('NIT', '900123456', '7', :razonSocial, 'RESPONSABLE_IVA',
                        'facturacion@vetsoftware.co', 1, :desde, :hasta, NOW(6), 0)
                """).setParameter("razonSocial", razonSocial).setParameter("desde", desde)
                .setParameter("hasta", hasta).executeUpdate();
        entityManager.flush();
    }

    /**
     * {@code economic_activities} no esta en {@code SchemaSeed}. El codigo es
     * sintetico porque {@code code} es {@code UNIQUE} global: uno real podria
     * chocar con la siembra de otra rodaja y el fallo no hablaria de esta feature.
     */
    private Long sembrarActividadEconomica() {
        entityManager.createNativeQuery("""
                INSERT INTO economic_activities (code, name, created_date, enabled, version)
                VALUES (:code, 'Software', NOW(6), true, 0)
                """).setParameter("code", CIIU_CODE).executeUpdate();
        entityManager.flush();
        return ((Number) entityManager
                .createNativeQuery("SELECT id FROM economic_activities WHERE code = :code")
                .setParameter("code", CIIU_CODE).getSingleResult()).longValue();
    }
}
