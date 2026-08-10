package com.vetsoftware.app.laboratorytest.domain;

import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BACTERIOLOGA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.BRANCH_ID;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CLINICA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CONSULTA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.CREADO;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.FECHA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.FIRULAIS;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.HEMOGRAMA;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.MICHI;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.PROCESADO;
import static com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother.UROANALISIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * El agregado es el unico sitio donde viven las reglas de la muestra: si una se
 * cae aqui, ningun service la vuelve a comprobar.
 */
@DisplayName("LaboratoryTest — invariantes y ciclo de vida del agregado")
class LaboratoryTestTest {

    private static final String DIAGNOSTICO_LIMITE = "d".repeat(2000);
    private static final String DIAGNOSTICO_EXCEDIDO = "d".repeat(2001);

    private static LaboratoryTest construir(LocalDate date, LaboratoryTestTypeRef testType,
            Integer quantity, String diagnosis, LaboratoryTestStatus status,
            LaboratoryTestPriority prioridad, AnimalRef animal, ConsultationRef consultation,
            CompanyRef company, Long branchId) {
        return new LaboratoryTest(LaboratoryTestMother.ID, date, testType, quantity, diagnosis,
                status, prioridad, animal, consultation, company, branchId, null, null, CREADO,
                true);
    }

    @Nested
    @DisplayName("Invariantes del constructor")
    class Invariantes {

        static Stream<Arguments> casosInvalidos() {
            return Stream.of(arguments("fecha ausente",
                    (ThrowingCallable) () -> construir(null, HEMOGRAMA, 1, null,
                            LaboratoryTestStatus.PENDING_COLLECTION, LaboratoryTestPriority.NORMAL,
                            FIRULAIS, CONSULTA, CLINICA, BRANCH_ID),
                    "date is required"),
                    arguments("tipo de examen ausente",
                            (ThrowingCallable) () -> construir(FECHA, null, 1, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA,
                                    BRANCH_ID),
                            "testType is required"),
                    arguments("cantidad ausente",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, null, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA,
                                    BRANCH_ID),
                            "quantity is required"),
                    arguments("cantidad cero",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, 0, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA,
                                    BRANCH_ID),
                            "quantity must be at least 1"),
                    arguments("cantidad negativa",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, -3, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA,
                                    BRANCH_ID),
                            "quantity must be at least 1"),
                    arguments("diagnostico de 2001 caracteres",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, 1,
                                    DIAGNOSTICO_EXCEDIDO, LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA,
                                    BRANCH_ID),
                            "diagnosis must be 2000 chars or less"),
                    arguments("estado ausente",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, 1, null, null,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA,
                                    BRANCH_ID),
                            "status is required"),
                    arguments("prioridad ausente",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, 1, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION, null, FIRULAIS,
                                    CONSULTA, CLINICA, BRANCH_ID),
                            "prioridad is required"),
                    arguments("animal ausente",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, 1, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, null, CONSULTA, CLINICA,
                                    BRANCH_ID),
                            "animal is required"),
                    arguments("empresa ausente",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, 1, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, null,
                                    BRANCH_ID),
                            "company is required"),
                    arguments("sede ausente",
                            (ThrowingCallable) () -> construir(FECHA, HEMOGRAMA, 1, null,
                                    LaboratoryTestStatus.PENDING_COLLECTION,
                                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA,
                                    null),
                            "branch is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("casosInvalidos")
        @DisplayName("rechaza los datos que la base no podria almacenar")
        void rechaza_los_datos_invalidos(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("una cantidad de 1 es el minimo aceptado")
        void una_cantidad_de_uno_es_el_minimo_aceptado() {
            LaboratoryTest muestra = construir(FECHA, HEMOGRAMA, 1, null,
                    LaboratoryTestStatus.PENDING_COLLECTION, LaboratoryTestPriority.NORMAL,
                    FIRULAIS, CONSULTA, CLINICA, BRANCH_ID);

            assertThat(muestra.getQuantity()).isEqualTo(1);
        }

        @Test
        @DisplayName("un diagnostico de exactamente 2000 caracteres si cabe")
        void un_diagnostico_de_exactamente_2000_caracteres_si_cabe() {
            LaboratoryTest muestra = construir(FECHA, HEMOGRAMA, 1, DIAGNOSTICO_LIMITE,
                    LaboratoryTestStatus.PENDING_COLLECTION, LaboratoryTestPriority.NORMAL,
                    FIRULAIS, CONSULTA, CLINICA, BRANCH_ID);

            assertThat(muestra.getDiagnosis()).hasSize(2000);
        }

        @Test
        @DisplayName("el diagnostico y la consulta son opcionales")
        void el_diagnostico_y_la_consulta_son_opcionales() {
            LaboratoryTest muestra = construir(FECHA, HEMOGRAMA, 1, null,
                    LaboratoryTestStatus.PENDING_COLLECTION, LaboratoryTestPriority.NORMAL,
                    FIRULAIS, null, CLINICA, BRANCH_ID);

            assertThat(muestra.getDiagnosis()).isNull();
            assertThat(muestra.getConsultation()).isNull();
        }

        @ParameterizedTest
        @EnumSource(LaboratoryTestStatus.class)
        @DisplayName("el constructor de rehidratacion admite cualquier estado persistido")
        void el_constructor_admite_cualquier_estado_persistido(LaboratoryTestStatus estado) {
            LaboratoryTest muestra = construir(FECHA, HEMOGRAMA, 1, null, estado,
                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA, BRANCH_ID);

            assertThat(muestra.getStatus()).isEqualTo(estado);
        }

        @Test
        @DisplayName("conserva todos los campos que recibe")
        void conserva_todos_los_campos_que_recibe() {
            LaboratoryTest muestra = LaboratoryTestMother.validada();

            assertThat(muestra.getId()).isEqualTo(LaboratoryTestMother.ID);
            assertThat(muestra.getDate()).isEqualTo(FECHA);
            assertThat(muestra.getTestType()).isEqualTo(HEMOGRAMA);
            assertThat(muestra.getQuantity()).isEqualTo(2);
            assertThat(muestra.getDiagnosis()).isEqualTo("Anemia regenerativa");
            assertThat(muestra.getStatus()).isEqualTo(LaboratoryTestStatus.COMPLETED);
            assertThat(muestra.getPrioridad()).isEqualTo(LaboratoryTestPriority.URGENTE);
            assertThat(muestra.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(muestra.getConsultation()).isEqualTo(CONSULTA);
            assertThat(muestra.getCompany()).isEqualTo(CLINICA);
            assertThat(muestra.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
            assertThat(muestra.getCreatedDate()).isEqualTo(CREADO);
            assertThat(muestra.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Alta de una muestra")
    class Creacion {

        @Test
        @DisplayName("la firma corta nace pendiente de toma, normal y habilitada")
        void la_firma_corta_nace_pendiente_de_toma() {
            LaboratoryTest muestra = LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control", FIRULAIS,
                    CONSULTA, CLINICA, BRANCH_ID);

            assertThat(muestra.getStatus()).isEqualTo(LaboratoryTestStatus.PENDING_COLLECTION);
            assertThat(muestra.getPrioridad()).isEqualTo(LaboratoryTestPriority.NORMAL);
            assertThat(muestra.isEnabled()).isTrue();
            assertThat(muestra.getProcessedBy()).isNull();
            assertThat(muestra.getProcessedDate()).isNull();
        }

        @Test
        @DisplayName("una muestra nueva no tiene id: lo asigna la persistencia")
        void una_muestra_nueva_no_tiene_id() {
            LaboratoryTest muestra = LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control", FIRULAIS,
                    CONSULTA, CLINICA, BRANCH_ID);

            assertThat(muestra.getId()).isNull();
        }

        @Test
        @DisplayName("sella la fecha de creacion en el momento del alta")
        void sella_la_fecha_de_creacion_en_el_momento_del_alta() {
            LaboratoryTest muestra = LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control", FIRULAIS,
                    CONSULTA, CLINICA, BRANCH_ID);

            assertThat(muestra.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(5, ChronoUnit.SECONDS));
        }

        @ParameterizedTest
        @EnumSource(value = LaboratoryTestStatus.class, names = {"PENDING_COLLECTION",
                "PENDING_PROCESSING"})
        @DisplayName("acepta como estado inicial solo los dos previos al procesamiento")
        void acepta_como_estado_inicial_los_dos_previos_al_procesamiento(
                LaboratoryTestStatus estado) {
            LaboratoryTest muestra = LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control", estado,
                    LaboratoryTestPriority.URGENTE, FIRULAIS, CONSULTA, CLINICA, BRANCH_ID, null,
                    null);

            assertThat(muestra.getStatus()).isEqualTo(estado);
            assertThat(muestra.getPrioridad()).isEqualTo(LaboratoryTestPriority.URGENTE);
        }

        @ParameterizedTest
        @EnumSource(value = LaboratoryTestStatus.class, names = {"PENDING_COLLECTION",
                "PENDING_PROCESSING"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("una muestra no puede nacer ya en proceso, validada o cancelada")
        void una_muestra_no_puede_nacer_avanzada(LaboratoryTestStatus estado) {
            assertThatThrownBy(() -> LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control", estado,
                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA, BRANCH_ID, null,
                    null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "initial status must be PENDING_COLLECTION or PENDING_PROCESSING");
        }

        @Test
        @DisplayName("un estado inicial nulo se rechaza antes que cualquier otro dato")
        void un_estado_inicial_nulo_se_rechaza() {
            assertThatThrownBy(() -> LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control", null,
                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA, BRANCH_ID, null,
                    null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "initial status must be PENDING_COLLECTION or PENDING_PROCESSING");
        }

        @Test
        @DisplayName("el alta tambien valida los invariantes del agregado")
        void el_alta_tambien_valida_los_invariantes() {
            assertThatThrownBy(() -> LaboratoryTest.create(FECHA, HEMOGRAMA, 0, "Control", FIRULAIS,
                    CONSULTA, CLINICA, BRANCH_ID)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be at least 1");
        }

        @Test
        @DisplayName("puede nacer con la firma de quien la recibio")
        void puede_nacer_con_la_firma_de_quien_la_recibio() {
            LaboratoryTest muestra = LaboratoryTest.create(FECHA, HEMOGRAMA, 1, "Control",
                    LaboratoryTestStatus.PENDING_PROCESSING, LaboratoryTestPriority.NORMAL,
                    FIRULAIS, CONSULTA, CLINICA, BRANCH_ID, BACTERIOLOGA, PROCESADO);

            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
        }
    }

    @Nested
    @DisplayName("Edicion")
    class Edicion {

        @Test
        @DisplayName("reemplaza los campos editables con los nuevos valores")
        void reemplaza_los_campos_editables() {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            muestra.update(LocalDate.of(2026, 4, 1), UROANALISIS, 3, "Nuevo diagnostico",
                    LaboratoryTestPriority.URGENTE, MICHI, null, CLINICA, BACTERIOLOGA, PROCESADO);

            assertThat(muestra.getDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(muestra.getTestType()).isEqualTo(UROANALISIS);
            assertThat(muestra.getQuantity()).isEqualTo(3);
            assertThat(muestra.getDiagnosis()).isEqualTo("Nuevo diagnostico");
            assertThat(muestra.getPrioridad()).isEqualTo(LaboratoryTestPriority.URGENTE);
            assertThat(muestra.getAnimal()).isEqualTo(MICHI);
            assertThat(muestra.getConsultation()).isNull();
            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
        }

        @Test
        @DisplayName("la edicion no mueve el estado, la sede ni la fecha de creacion")
        void la_edicion_no_mueve_el_estado_la_sede_ni_la_creacion() {
            LaboratoryTest muestra = LaboratoryTestMother.validada();

            muestra.update(FECHA, UROANALISIS, 1, null, LaboratoryTestPriority.NORMAL, FIRULAIS,
                    CONSULTA, CLINICA, null, null);

            assertThat(muestra.getStatus()).isEqualTo(LaboratoryTestStatus.COMPLETED);
            assertThat(muestra.getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(muestra.getCreatedDate()).isEqualTo(CREADO);
        }

        @Test
        @DisplayName("editar con datos invalidos deja el agregado intacto")
        void editar_con_datos_invalidos_deja_el_agregado_intacto() {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            assertThatThrownBy(() -> muestra.update(FECHA, UROANALISIS, 0, null,
                    LaboratoryTestPriority.URGENTE, MICHI, null, CLINICA, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be at least 1");

            assertThat(muestra.getTestType()).isEqualTo(HEMOGRAMA);
            assertThat(muestra.getQuantity()).isEqualTo(1);
            assertThat(muestra.getAnimal()).isEqualTo(FIRULAIS);
            assertThat(muestra.getPrioridad()).isEqualTo(LaboratoryTestPriority.NORMAL);
        }

        @Test
        @DisplayName("editar sin empresa se rechaza")
        void editar_sin_empresa_se_rechaza() {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            assertThatThrownBy(() -> muestra.update(FECHA, HEMOGRAMA, 1, null,
                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("company is required");
        }

        @Test
        @DisplayName("editar puede borrar la firma de procesamiento")
        void editar_puede_borrar_la_firma_de_procesamiento() {
            LaboratoryTest muestra = LaboratoryTestMother.validada();

            muestra.update(FECHA, HEMOGRAMA, 1, null, LaboratoryTestPriority.NORMAL, FIRULAIS,
                    CONSULTA, CLINICA, null, null);

            assertThat(muestra.getProcessedBy()).isNull();
            assertThat(muestra.getProcessedDate()).isNull();
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @ParameterizedTest
        @EnumSource(LaboratoryTestStatus.class)
        @DisplayName("acepta cualquier estado destino del catalogo")
        void acepta_cualquier_estado_destino(LaboratoryTestStatus destino) {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            muestra.changeStatus(destino);

            assertThat(muestra.getStatus()).isEqualTo(destino);
        }

        @Test
        @DisplayName("un estado nulo no puede sustituir al actual")
        void un_estado_nulo_no_puede_sustituir_al_actual() {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            assertThatThrownBy(() -> muestra.changeStatus(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");

            assertThat(muestra.getStatus()).isEqualTo(LaboratoryTestStatus.PENDING_COLLECTION);
        }

        @Test
        @DisplayName("cambiar de estado sin firma no toca quien proceso la muestra")
        void cambiar_de_estado_sin_firma_no_toca_el_procesador() {
            LaboratoryTest muestra = LaboratoryTestMother.validada();

            muestra.changeStatus(LaboratoryTestStatus.IN_PROGRESS);

            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
        }

        @Test
        @DisplayName("la transicion firmada registra quien valido y cuando")
        void la_transicion_firmada_registra_quien_valido_y_cuando() {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            muestra.changeStatus(LaboratoryTestStatus.PENDING_VALIDATION, BACTERIOLOGA, PROCESADO);

            assertThat(muestra.getStatus()).isEqualTo(LaboratoryTestStatus.PENDING_VALIDATION);
            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
        }

        @Test
        @DisplayName("la transicion firmada con estado nulo no borra la firma anterior")
        void la_transicion_firmada_con_estado_nulo_no_borra_la_firma() {
            LaboratoryTest muestra = LaboratoryTestMother.validada();

            assertThatThrownBy(() -> muestra.changeStatus(null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");

            assertThat(muestra.getProcessedBy()).isEqualTo(BACTERIOLOGA);
            assertThat(muestra.getProcessedDate()).isEqualTo(PROCESADO);
        }
    }

    @Nested
    @DisplayName("Borrado logico")
    class BorradoLogico {

        @Test
        @DisplayName("deshabilitar apaga la bandera sin perder los datos")
        void deshabilitar_apaga_la_bandera() {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            muestra.disable();

            assertThat(muestra.isEnabled()).isFalse();
            assertThat(muestra.getAnimal()).isEqualTo(FIRULAIS);
        }

        @Test
        @DisplayName("reactivar vuelve a encender la bandera")
        void reactivar_vuelve_a_encender_la_bandera() {
            LaboratoryTest muestra = LaboratoryTestMother.deshabilitada();

            muestra.enable();

            assertThat(muestra.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("deshabilitar dos veces es idempotente")
        void deshabilitar_dos_veces_es_idempotente() {
            LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();

            muestra.disable();
            muestra.disable();

            assertThat(muestra.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("una muestra deshabilitada sigue siendo editable")
        void una_muestra_deshabilitada_sigue_siendo_editable() {
            LaboratoryTest muestra = LaboratoryTestMother.deshabilitada();

            assertThatCode(() -> muestra.update(FECHA, UROANALISIS, 2, null,
                    LaboratoryTestPriority.NORMAL, FIRULAIS, CONSULTA, CLINICA, null, null))
                    .doesNotThrowAnyException();
        }
    }
}
