package com.vetsoftware.app.vaccination.domain;

import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.CLINICA;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.CONSULTA;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.CREADO;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.FECHA;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.FIRULAIS;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.MICHI;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.MOQUILLO;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.OTRA_CONSULTA;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.PROXIMA;
import static com.vetsoftware.app.vaccination.testsupport.VaccinationMother.RABIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.vaccination.testsupport.VaccinationMother;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Vaccination")
class VaccinationTest {

    private static final String LOTE_100 = "L".repeat(100);
    private static final String LOTE_101 = "L".repeat(101);
    private static final String TEXTO_2000 = "n".repeat(2000);
    private static final String TEXTO_2001 = "n".repeat(2001);
    private static final String VIA_30 = "v".repeat(30);
    private static final String VIA_31 = "v".repeat(31);
    private static final String SITIO_60 = "s".repeat(60);
    private static final String SITIO_61 = "s".repeat(61);

    private static Vaccination nueva(LocalDate date, VaccinationTypeRef type, String lot,
            String notes, String route, String site, AnimalRef animal, CompanyRef company) {
        return new Vaccination(1L, date, type, lot, notes, route, site, PROXIMA, animal, CONSULTA,
                company, CREADO, true);
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada dato tal como se recibio")
        void conserva_cada_dato() {
            Vaccination vaccination = VaccinationMother.vigente();

            assertThat(vaccination.getId()).isEqualTo(VaccinationMother.VACCINATION_ID);
            assertThat(vaccination.getDate()).isEqualTo(FECHA);
            assertThat(vaccination.getVaccinationType()).isEqualTo(RABIA);
            assertThat(vaccination.getLot()).isEqualTo("L-2026-A");
            assertThat(vaccination.getNotes()).isEqualTo("Sin reaccion");
            assertThat(vaccination.getRoute()).isEqualTo("Subcutanea");
            assertThat(vaccination.getApplicationSite()).isEqualTo("Cuello");
            assertThat(vaccination.getNextVaccination()).isEqualTo(PROXIMA);
            assertThat(vaccination.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(vaccination.getConsultation()).isEqualTo(CONSULTA);
            assertThat(vaccination.getCompany()).isEqualTo(CLINICA);
            assertThat(vaccination.getCreatedDate()).isEqualTo(CREADO);
            assertThat(vaccination.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("acepta una vacuna sin consulta asociada")
        void acepta_sin_consulta() {
            assertThat(VaccinationMother.sinConsulta().getConsultation()).isNull();
        }

        @Test
        @DisplayName("normaliza a null la via y el sitio en blanco")
        void normaliza_a_null_via_y_sitio_en_blanco() {
            Vaccination vaccination = nueva(FECHA, RABIA, "L-1", null, "   ", "", FIRULAIS,
                    CLINICA);

            assertThat(vaccination.getRoute()).isNull();
            assertThat(vaccination.getApplicationSite()).isNull();
        }

        @Test
        @DisplayName("recorta los espacios de la via y del sitio")
        void recorta_espacios_de_via_y_sitio() {
            Vaccination vaccination = nueva(FECHA, RABIA, "L-1", null, "  Subcutanea  ",
                    "  Cuello  ", FIRULAIS, CLINICA);

            assertThat(vaccination.getRoute()).isEqualTo("Subcutanea");
            assertThat(vaccination.getApplicationSite()).isEqualTo("Cuello");
        }

        @Test
        @DisplayName("acepta los valores justo en el limite de longitud")
        void acepta_los_limites_exactos() {
            Vaccination vaccination = nueva(FECHA, RABIA, LOTE_100, TEXTO_2000, VIA_30, SITIO_60,
                    FIRULAIS, CLINICA);

            assertThat(vaccination.getLot()).hasSize(100);
            assertThat(vaccination.getNotes()).hasSize(2000);
            assertThat(vaccination.getRoute()).hasSize(30);
            assertThat(vaccination.getApplicationSite()).hasSize(60);
        }
    }

    @Nested
    @DisplayName("factory create")
    class Creacion {

        @Test
        @DisplayName("nace sin id, habilitada y con fecha de creacion")
        void nace_sin_id_y_habilitada() {
            Vaccination vaccination = Vaccination.create(FECHA, RABIA, "L-2026-A", "Sin reaccion",
                    "Subcutanea", "Cuello", PROXIMA, FIRULAIS, CONSULTA, CLINICA);

            assertThat(vaccination.getId()).isNull();
            assertThat(vaccination.isEnabled()).isTrue();
            assertThat(vaccination.getCreatedDate()).isNotNull();
            assertThat(vaccination.getVaccinationType()).isEqualTo(RABIA);
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("reemplaza los datos y las referencias sin tocar id ni fecha de creacion")
        void reemplaza_datos_sin_tocar_id_ni_creacion() {
            Vaccination vaccination = VaccinationMother.vigente();

            vaccination.update(LocalDate.of(2026, 3, 1), MOQUILLO, "L-2026-B", "Reaccion leve",
                    "Intramuscular", "Muslo", LocalDate.of(2027, 3, 1), MICHI, OTRA_CONSULTA,
                    CLINICA);

            assertThat(vaccination.getId()).isEqualTo(VaccinationMother.VACCINATION_ID);
            assertThat(vaccination.getCreatedDate()).isEqualTo(CREADO);
            assertThat(vaccination.getDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(vaccination.getVaccinationType()).isEqualTo(MOQUILLO);
            assertThat(vaccination.getLot()).isEqualTo("L-2026-B");
            assertThat(vaccination.getNotes()).isEqualTo("Reaccion leve");
            assertThat(vaccination.getRoute()).isEqualTo("Intramuscular");
            assertThat(vaccination.getApplicationSite()).isEqualTo("Muslo");
            assertThat(vaccination.getNextVaccination()).isEqualTo(LocalDate.of(2027, 3, 1));
            assertThat(vaccination.getAnimal()).isEqualTo(MICHI);
            assertThat(vaccination.getConsultation()).isEqualTo(OTRA_CONSULTA);
        }

        @Test
        @DisplayName("permite desasociar la consulta")
        void permite_desasociar_la_consulta() {
            Vaccination vaccination = VaccinationMother.vigente();

            vaccination.update(FECHA, RABIA, "L-2026-A", null, null, null, null, FIRULAIS, null,
                    CLINICA);

            assertThat(vaccination.getConsultation()).isNull();
        }

        @Test
        @DisplayName("no cambia el estado de habilitacion")
        void no_cambia_el_estado_de_habilitacion() {
            Vaccination vaccination = VaccinationMother.deshabilitada();

            vaccination.update(FECHA, RABIA, "L-2026-A", null, null, null, null, FIRULAIS, null,
                    CLINICA);

            assertThat(vaccination.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("rechaza datos invalidos y deja la vacuna intacta")
        void rechaza_datos_invalidos_y_deja_la_vacuna_intacta() {
            Vaccination vaccination = VaccinationMother.vigente();

            assertThatThrownBy(() -> vaccination.update(FECHA, RABIA, "  ", null, null, null, null,
                    FIRULAIS, null, CLINICA)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lot is required");
            assertThat(vaccination.getLot()).isEqualTo("L-2026-A");
        }
    }

    @Nested
    @DisplayName("habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable deja la vacuna deshabilitada")
        void disable_deja_la_vacuna_deshabilitada() {
            Vaccination vaccination = VaccinationMother.vigente();

            vaccination.disable();

            assertThat(vaccination.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("enable vuelve a habilitar una vacuna deshabilitada")
        void enable_vuelve_a_habilitar() {
            Vaccination vaccination = VaccinationMother.deshabilitada();

            vaccination.enable();

            assertThat(vaccination.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("invariantes")
    class Invariantes {

        static Stream<Arguments> invariantes() {
            return Stream.of(
                    Arguments.of("fecha nula",
                            (ThrowingCallable) () -> nueva(null, RABIA, "L-1", null, null, null,
                                    FIRULAIS, CLINICA),
                            "date is required"),
                    Arguments.of("tipo de vacuna nulo",
                            (ThrowingCallable) () -> nueva(FECHA, null, "L-1", null, null, null,
                                    FIRULAIS, CLINICA),
                            "vaccinationType is required"),
                    Arguments.of("lote nulo",
                            (ThrowingCallable) () -> nueva(FECHA, RABIA, null, null, null, null,
                                    FIRULAIS, CLINICA),
                            "lot is required"),
                    Arguments.of("lote en blanco",
                            (ThrowingCallable) () -> nueva(FECHA, RABIA, "   ", null, null, null,
                                    FIRULAIS, CLINICA),
                            "lot is required"),
                    Arguments.of("lote de 101 caracteres",
                            (ThrowingCallable) () -> nueva(FECHA, RABIA, LOTE_101, null, null, null,
                                    FIRULAIS, CLINICA),
                            "lot must be 100 chars or less"),
                    Arguments.of("notas de 2001 caracteres",
                            (ThrowingCallable) () -> nueva(FECHA, RABIA, "L-1", TEXTO_2001, null,
                                    null, FIRULAIS, CLINICA),
                            "notes must be 2000 chars or less"),
                    Arguments.of("via de 31 caracteres",
                            (ThrowingCallable) () -> nueva(FECHA, RABIA, "L-1", null, VIA_31, null,
                                    FIRULAIS, CLINICA),
                            "route must be 30 chars or less"),
                    Arguments.of("sitio de aplicacion de 61 caracteres",
                            (ThrowingCallable) () -> nueva(FECHA, RABIA, "L-1", null, null,
                                    SITIO_61, FIRULAIS, CLINICA),
                            "applicationSite must be 60 chars or less"),
                    Arguments.of("animal nulo",
                            (ThrowingCallable) () -> nueva(FECHA, RABIA, "L-1", null, null, null,
                                    null, CLINICA),
                            "animal is required"),
                    Arguments.of("empresa nula", (ThrowingCallable) () -> nueva(FECHA, RABIA, "L-1",
                            null, null, null, FIRULAIS, null), "company is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invariantes")
        @DisplayName("el constructor rechaza")
        void el_constructor_rechaza(String caso, ThrowingCallable ejecucion, String mensaje) {
            assertThatThrownBy(ejecucion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }
    }
}
