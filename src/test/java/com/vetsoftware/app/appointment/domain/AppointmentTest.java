package com.vetsoftware.app.appointment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.appointment.testsupport.AppointmentMother;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Appointment — invariantes, agenda y ciclo de vida")
class AppointmentTest {

    private static final LocalDateTime INICIO = AppointmentMother.INICIO;
    private static final LocalDateTime CREADA = AppointmentMother.CREADA;

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir
     * diecisiete argumentos en cada escenario invalido, que es como se cuela un
     * test que valida un campo distinto del que dice validar.
     */
    private static Builder valida() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = AppointmentMother.APPOINTMENT_ID;
        private LocalDateTime startAt = INICIO;
        private AppointmentType type = AppointmentType.CONSULTATION;
        private AppointmentStatus status = AppointmentStatus.REQUESTED;
        private String notes = "Control anual";
        private String cancellationReason;
        private AnimalRef animal = AppointmentMother.FIRULAIS;
        private OwnerRef owner = AppointmentMother.DUENO;
        private String clientName;
        private String clientPhone;
        private String clientEmail;
        private EmployeeRef employee = AppointmentMother.VETERINARIA;
        private CompanyRef company = AppointmentMother.CLINICA;
        private BranchRef branch = AppointmentMother.PRINCIPAL;

        private Builder startAt(LocalDateTime value) {
            this.startAt = value;
            return this;
        }

        private Builder type(AppointmentType value) {
            this.type = value;
            return this;
        }

        private Builder status(AppointmentStatus value) {
            this.status = value;
            return this;
        }

        private Builder notes(String value) {
            this.notes = value;
            return this;
        }

        private Builder cancellationReason(String value) {
            this.cancellationReason = value;
            return this;
        }

        private Builder animal(AnimalRef value) {
            this.animal = value;
            return this;
        }

        private Builder owner(OwnerRef value) {
            this.owner = value;
            return this;
        }

        private Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        private Builder clientPhone(String value) {
            this.clientPhone = value;
            return this;
        }

        private Builder clientEmail(String value) {
            this.clientEmail = value;
            return this;
        }

        private Builder employee(EmployeeRef value) {
            this.employee = value;
            return this;
        }

        private Builder company(CompanyRef value) {
            this.company = value;
            return this;
        }

        private Builder branch(BranchRef value) {
            this.branch = value;
            return this;
        }

        private Appointment build() {
            return new Appointment(id, startAt, type, status, notes, cancellationReason, animal,
                    owner, clientName, clientPhone, clientEmail, employee, company, branch, 3L,
                    true, CREADA);
        }
    }

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("conserva todos los datos de la cita tal cual se recibieron")
        void conserva_todos_los_datos() {
            Appointment cita = valida().clientPhone("3001234567").build();

            assertThat(cita.getId()).isEqualTo(AppointmentMother.APPOINTMENT_ID);
            assertThat(cita.getStartAt()).isEqualTo(INICIO);
            assertThat(cita.getType()).isEqualTo(AppointmentType.CONSULTATION);
            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
            assertThat(cita.getNotes()).isEqualTo("Control anual");
            assertThat(cita.getAnimal()).isEqualTo(AppointmentMother.FIRULAIS);
            assertThat(cita.getOwner()).isEqualTo(AppointmentMother.DUENO);
            assertThat(cita.getClientPhone()).isEqualTo("3001234567");
            assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.VETERINARIA);
            assertThat(cita.getCompany()).isEqualTo(AppointmentMother.CLINICA);
            assertThat(cita.getBranch()).isEqualTo(AppointmentMother.PRINCIPAL);
            assertThat(cita.getVersion()).isEqualTo(3L);
            assertThat(cita.isEnabled()).isTrue();
            assertThat(cita.getCreatedDate()).isEqualTo(CREADA);
        }

        @Test
        @DisplayName("un estado nulo se normaliza a REQUESTED, nunca queda sin estado")
        void estado_nulo_se_normaliza_a_requested() {
            assertThat(valida().status(null).build().getStatus())
                    .isEqualTo(AppointmentStatus.REQUESTED);
        }

        @Test
        @DisplayName("los textos en blanco se guardan como null y no como cadena vacia")
        void los_textos_en_blanco_se_guardan_como_null() {
            Appointment cita = valida().notes("   ").clientName("  ").clientPhone("")
                    .clientEmail("   ").cancellationReason("  ").build();

            assertThat(cita.getNotes()).isNull();
            assertThat(cita.getClientName()).isNull();
            assertThat(cita.getClientPhone()).isNull();
            assertThat(cita.getClientEmail()).isNull();
            assertThat(cita.getCancellationReason()).isNull();
        }

        @Test
        @DisplayName("acepta una cita de contacto libre sin animal ni propietario")
        void acepta_contacto_libre_sin_animal_ni_propietario() {
            Appointment cita = valida().animal(null).owner(null).clientName("Walk-in").build();

            assertThat(cita.getAnimal()).isNull();
            assertThat(cita.getOwner()).isNull();
            assertThat(cita.getClientName()).isEqualTo("Walk-in");
        }

        @Test
        @DisplayName("acepta una cita cuyo unico sujeto es el propietario")
        void acepta_cita_cuyo_unico_sujeto_es_el_propietario() {
            assertThatCode(() -> valida().animal(null).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("acepta una cita cuyo unico sujeto es el animal")
        void acepta_cita_cuyo_unico_sujeto_es_el_animal() {
            assertThatCode(() -> valida().owner(null).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        static Stream<Arguments> camposObligatorios() {
            return Stream.of(arguments("sin fecha de inicio",
                    (ThrowingCallable) () -> valida().startAt(null).build(), "startAt is required"),
                    arguments("sin tipo", (ThrowingCallable) () -> valida().type(null).build(),
                            "type is required"),
                    arguments("sin veterinario",
                            (ThrowingCallable) () -> valida().employee(null).build(),
                            "employee is required"),
                    arguments("sin empresa",
                            (ThrowingCallable) () -> valida().company(null).build(),
                            "company is required"),
                    arguments("sin sede", (ThrowingCallable) () -> valida().branch(null).build(),
                            "branch is required"),
                    arguments("sin ningun sujeto",
                            (ThrowingCallable) () -> valida().animal(null).owner(null)
                                    .clientName(null).build(),
                            "at least one of {animal, owner, clientName} is required"),
                    arguments("con nombre de contacto en blanco y sin animal ni dueno",
                            (ThrowingCallable) () -> valida().animal(null).owner(null)
                                    .clientName("   ").build(),
                            "at least one of {animal, owner, clientName} is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("camposObligatorios")
        @DisplayName("rechaza la cita cuando falta un dato obligatorio")
        void rechaza_la_cita_cuando_falta_un_dato_obligatorio(String caso, ThrowingCallable accion,
                String mensaje) {
            assertThatThrownBy(accion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        static Stream<Arguments> longitudesMaximas() {
            return Stream.of(
                    arguments("notas",
                            (ThrowingCallable) () -> valida().notes("n".repeat(1001)).build(),
                            "notes must be 1000 chars or less"),
                    arguments("nombre del contacto",
                            (ThrowingCallable) () -> valida().clientName("c".repeat(121)).build(),
                            "clientName must be 120 chars or less"),
                    arguments("telefono del contacto",
                            (ThrowingCallable) () -> valida().clientPhone("3".repeat(31)).build(),
                            "clientPhone must be 30 chars or less"),
                    arguments("correo del contacto",
                            (ThrowingCallable) () -> valida().clientEmail("e".repeat(151)).build(),
                            "clientEmail must be 150 chars or less"),
                    arguments(
                            "motivo de cancelacion", (ThrowingCallable) () -> valida()
                                    .cancellationReason("m".repeat(301)).build(),
                            "cancellationReason must be 300 chars or less"));
        }

        @ParameterizedTest(name = "{0} por encima del tope")
        @MethodSource("longitudesMaximas")
        @DisplayName("rechaza los textos que superan su longitud maxima")
        void rechaza_los_textos_que_superan_su_longitud_maxima(String caso, ThrowingCallable accion,
                String mensaje) {
            assertThatThrownBy(accion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta exactamente el limite de cada texto: 1000/120/30/150/300")
        void acepta_exactamente_el_limite_de_cada_texto() {
            assertThatCode(() -> valida().notes("n".repeat(1000)).clientName("c".repeat(120))
                    .clientPhone("3".repeat(30)).clientEmail("e".repeat(150))
                    .cancellationReason("m".repeat(300)).build()).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Alta")
    class Alta {

        @Test
        @DisplayName("create deja la cita solicitada, habilitada, sin id y sin motivo de cancelacion")
        void create_deja_la_cita_solicitada_y_habilitada() {
            Appointment cita = Appointment.create(INICIO, AppointmentType.VACCINATION, "Refuerzo",
                    AppointmentMother.FIRULAIS, AppointmentMother.DUENO, null, null, null,
                    AppointmentMother.VETERINARIA, AppointmentMother.CLINICA,
                    AppointmentMother.PRINCIPAL);

            assertThat(cita.getId()).isNull();
            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
            assertThat(cita.getVersion()).isZero();
            assertThat(cita.isEnabled()).isTrue();
            assertThat(cita.getCancellationReason()).isNull();
            assertThat(cita.getNotes()).isEqualTo("Refuerzo");
        }

        @Test
        @DisplayName("create aplica las mismas invariantes que el constructor")
        void create_aplica_las_mismas_invariantes() {
            assertThatThrownBy(() -> Appointment.create(INICIO, AppointmentType.VACCINATION, null,
                    null, null, null, null, null, AppointmentMother.VETERINARIA,
                    AppointmentMother.CLINICA, AppointmentMother.PRINCIPAL))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one of");
        }
    }

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("update reemplaza los datos editables y conserva empresa, sede y estado")
        void update_reemplaza_los_datos_editables() {
            Appointment cita = valida().build();

            cita.update(AppointmentMother.NUEVO_INICIO, AppointmentType.SURGERY, "Cirugia", null,
                    null, "Walk-in", "3009998888", "walkin@example.com",
                    AppointmentMother.OTRO_VETERINARIO);

            assertThat(cita.getStartAt()).isEqualTo(AppointmentMother.NUEVO_INICIO);
            assertThat(cita.getType()).isEqualTo(AppointmentType.SURGERY);
            assertThat(cita.getNotes()).isEqualTo("Cirugia");
            assertThat(cita.getAnimal()).isNull();
            assertThat(cita.getOwner()).isNull();
            assertThat(cita.getClientName()).isEqualTo("Walk-in");
            assertThat(cita.getClientPhone()).isEqualTo("3009998888");
            assertThat(cita.getClientEmail()).isEqualTo("walkin@example.com");
            assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.OTRO_VETERINARIO);
            assertThat(cita.getCompany()).isEqualTo(AppointmentMother.CLINICA);
            assertThat(cita.getBranch()).isEqualTo(AppointmentMother.PRINCIPAL);
            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        }

        @Test
        @DisplayName("update rechaza dejar la cita sin ningun sujeto y no modifica nada")
        void update_rechaza_dejar_la_cita_sin_sujeto() {
            Appointment cita = valida().build();

            assertThatThrownBy(
                    () -> cita.update(AppointmentMother.NUEVO_INICIO, AppointmentType.SURGERY, null,
                            null, null, null, null, null, AppointmentMother.VETERINARIA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one of");

            assertThat(cita.getStartAt()).isEqualTo(INICIO);
            assertThat(cita.getAnimal()).isEqualTo(AppointmentMother.FIRULAIS);
        }

        @Test
        @DisplayName("update exige veterinario asignado")
        void update_exige_veterinario_asignado() {
            Appointment cita = valida().build();

            assertThatThrownBy(() -> cita.update(INICIO, AppointmentType.CONTROL, null,
                    AppointmentMother.FIRULAIS, null, null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee is required");
        }

        @Test
        @DisplayName("update normaliza a null las notas en blanco")
        void update_normaliza_a_null_las_notas_en_blanco() {
            Appointment cita = valida().build();

            cita.update(INICIO, AppointmentType.CONTROL, "   ", AppointmentMother.FIRULAIS, null,
                    null, null, null, AppointmentMother.VETERINARIA);

            assertThat(cita.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("Reprogramacion")
    class Reprogramacion {

        @Test
        @DisplayName("reschedule mueve la hora y reasigna el veterinario sin tocar el estado")
        void reschedule_mueve_la_hora_y_reasigna_el_veterinario() {
            Appointment cita = valida().status(AppointmentStatus.CONFIRMED).build();

            cita.reschedule(AppointmentMother.NUEVO_INICIO, AppointmentMother.OTRO_VETERINARIO);

            assertThat(cita.getStartAt()).isEqualTo(AppointmentMother.NUEVO_INICIO);
            assertThat(cita.getEmployee()).isEqualTo(AppointmentMother.OTRO_VETERINARIO);
            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        }

        @Test
        @DisplayName("reschedule exige nueva hora de inicio")
        void reschedule_exige_nueva_hora_de_inicio() {
            Appointment cita = valida().build();

            assertThatThrownBy(() -> cita.reschedule(null, AppointmentMother.VETERINARIA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("startAt is required");
        }

        @Test
        @DisplayName("reschedule exige veterinario y deja la cita intacta si falta")
        void reschedule_exige_veterinario() {
            Appointment cita = valida().build();

            assertThatThrownBy(() -> cita.reschedule(AppointmentMother.NUEVO_INICIO, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employee is required");

            assertThat(cita.getStartAt()).isEqualTo(INICIO);
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @Test
        @DisplayName("transitionTo avanza al siguiente estado permitido")
        void transition_to_avanza_al_siguiente_estado_permitido() {
            Appointment cita = valida().status(AppointmentStatus.CONFIRMED).build();

            cita.transitionTo(AppointmentStatus.ARRIVED);

            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.ARRIVED);
        }

        @Test
        @DisplayName("transitionTo exige un estado destino")
        void transition_to_exige_un_estado_destino() {
            Appointment cita = valida().build();

            assertThatThrownBy(() -> cita.transitionTo(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");
        }

        @Test
        @DisplayName("transitionTo rechaza un salto no permitido con el origen y el destino en el mensaje")
        void transition_to_rechaza_un_salto_no_permitido() {
            Appointment cita = valida().status(AppointmentStatus.REQUESTED).build();

            assertThatThrownBy(() -> cita.transitionTo(AppointmentStatus.COMPLETED))
                    .isInstanceOf(InvalidAppointmentTransitionException.class)
                    .hasMessageContaining("REQUESTED").hasMessageContaining("COMPLETED");

            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        }

        @ParameterizedTest
        @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "NO_SHOW", "CANCELLED"})
        @DisplayName("una cita en estado terminal no admite ninguna transicion")
        void una_cita_terminal_no_admite_transiciones(AppointmentStatus terminal) {
            Appointment cita = valida().status(terminal).build();

            assertThatThrownBy(() -> cita.transitionTo(AppointmentStatus.CONFIRMED))
                    .isInstanceOf(InvalidAppointmentTransitionException.class);
        }

        @Test
        @DisplayName("transitionTo(CANCELLED) cancela conservando el motivo ya registrado")
        void transition_to_cancelled_conserva_el_motivo_registrado() {
            Appointment cita = valida().status(AppointmentStatus.CONFIRMED)
                    .cancellationReason("El dueno aviso ayer").build();

            cita.transitionTo(AppointmentStatus.CANCELLED);

            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(cita.getCancellationReason()).isEqualTo("El dueno aviso ayer");
        }

        @Test
        @DisplayName("transitionTo(CANCELLED) sobre una cita ya cancelada falla como transicion invalida")
        void transition_to_cancelled_sobre_cancelada_falla() {
            Appointment cita = valida().status(AppointmentStatus.CANCELLED).build();

            assertThatThrownBy(() -> cita.transitionTo(AppointmentStatus.CANCELLED))
                    .isInstanceOf(InvalidAppointmentTransitionException.class)
                    .hasMessageContaining("CANCELLED -> CANCELLED");
        }
    }

    @Nested
    @DisplayName("Cancelacion")
    class Cancelacion {

        @ParameterizedTest
        @EnumSource(value = AppointmentStatus.class, names = {"REQUESTED", "CONFIRMED", "ARRIVED",
                "IN_PROGRESS"})
        @DisplayName("cancela desde cualquier estado no terminal y registra el motivo")
        void cancela_desde_cualquier_estado_no_terminal(AppointmentStatus origen) {
            Appointment cita = valida().status(origen).build();

            cita.cancel("El dueno no puede asistir");

            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(cita.getCancellationReason()).isEqualTo("El dueno no puede asistir");
        }

        @ParameterizedTest
        @EnumSource(value = AppointmentStatus.class, names = {"COMPLETED", "NO_SHOW", "CANCELLED"})
        @DisplayName("no cancela una cita ya terminal")
        void no_cancela_una_cita_ya_terminal(AppointmentStatus terminal) {
            Appointment cita = valida().status(terminal).build();

            assertThatThrownBy(() -> cita.cancel("tarde"))
                    .isInstanceOf(InvalidAppointmentTransitionException.class);

            assertThat(cita.getStatus()).isEqualTo(terminal);
        }

        @Test
        @DisplayName("un motivo en blanco se guarda como null")
        void un_motivo_en_blanco_se_guarda_como_null() {
            Appointment cita = valida().build();

            cita.cancel("   ");

            assertThat(cita.getCancellationReason()).isNull();
        }

        @Test
        @DisplayName("rechaza un motivo de mas de 300 caracteres y no cancela")
        void rechaza_un_motivo_demasiado_largo() {
            Appointment cita = valida().build();

            assertThatThrownBy(() -> cita.cancel("m".repeat(301)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cancellationReason must be 300 chars or less");

            assertThat(cita.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        }

        @Test
        @DisplayName("acepta un motivo de exactamente 300 caracteres")
        void acepta_un_motivo_de_exactamente_300_caracteres() {
            Appointment cita = valida().build();

            cita.cancel("m".repeat(300));

            assertThat(cita.getCancellationReason()).hasSize(300);
        }
    }

    @Nested
    @DisplayName("Habilitacion")
    class Habilitacion {

        @Test
        @DisplayName("disable y enable alternan la marca de activa")
        void disable_y_enable_alternan_la_marca_de_activa() {
            Appointment cita = valida().build();

            cita.disable();
            assertThat(cita.isEnabled()).isFalse();

            cita.enable();
            assertThat(cita.isEnabled()).isTrue();
        }
    }
}
