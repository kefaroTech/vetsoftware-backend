package com.vetsoftware.app.appointment.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.appointment.application.dto.AppointmentConfirmationData;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.infrastructure.email.ResendEmailClient;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * No mockea {@code DATE_FMT}/{@code TIME_FMT}: son formateadores fijos en
 * castellano de Colombia y su salida es determinista para una fecha fija, así
 * que se afirma directamente sobre el texto que produce.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResendAppointmentConfirmationEmailSender — correo de confirmacion de cita")
class ResendAppointmentConfirmationEmailSenderTest {

    private static final String TEMPLATE_ID = "tpl_confirmacion";
    // Jueves 20 de agosto de 2026, 09:30.
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 8, 20, 9, 30);

    @Mock
    private ResendEmailClient email;

    private ResendAppointmentConfirmationEmailSender sender;

    @BeforeEach
    void crearSender() {
        sender = new ResendAppointmentConfirmationEmailSender(email, TEMPLATE_ID);
    }

    private static AppointmentConfirmationData datosCompletos() {
        return new AppointmentConfirmationData("ana@example.com", "Ana Ruiz", "Clinica Norte",
                INICIO, AppointmentType.CONSULTATION, "Dra. Vet", "Firulais", "Sede Norte",
                "Calle 1 #2-3", "Traer carnet de vacunas");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enviarYCapturarVariables(AppointmentConfirmationData datos) {
        sender.send(datos);
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(email).sendTemplate(eq(datos.recipientEmail()), isNull(),
                org.mockito.ArgumentMatchers.any(), eq(TEMPLATE_ID), variables.capture());
        return variables.getValue();
    }

    @Nested
    @DisplayName("plantilla, destinatario y asunto")
    class PlantillaYAsunto {

        @Test
        @DisplayName("envia la plantilla configurada al correo del destinatario, con el asunto de la empresa")
        void envia_la_plantilla_al_destinatario_con_el_asunto_de_la_empresa() {
            sender.send(datosCompletos());

            verify(email).sendTemplate(eq("ana@example.com"), isNull(),
                    eq("Tu cita en Clinica Norte quedó agendada"), eq(TEMPLATE_ID),
                    org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("sin nombre de empresa el asunto y la variable caen al generico 'tu clinica'")
        void sin_nombre_de_empresa_cae_al_generico() {
            AppointmentConfirmationData datos = new AppointmentConfirmationData("ana@example.com",
                    "Ana Ruiz", null, INICIO, AppointmentType.CONSULTATION, "Dra. Vet", "Firulais",
                    "Sede Norte", "Calle 1 #2-3", "Notas");

            Map<String, Object> variables = enviarYCapturarVariables(datos);

            verify(email).sendTemplate(eq("ana@example.com"), isNull(),
                    eq("Tu cita en tu clínica quedó agendada"), eq(TEMPLATE_ID),
                    org.mockito.ArgumentMatchers.any());
            assertThat(variables).containsEntry("COMPANY_NAME", "tu clínica");
        }

        @Test
        @DisplayName("un nombre de empresa en blanco tambien cae al generico")
        void nombre_de_empresa_en_blanco_cae_al_generico() {
            AppointmentConfirmationData datos = new AppointmentConfirmationData("ana@example.com",
                    "Ana Ruiz", "   ", INICIO, AppointmentType.CONSULTATION, "Dra. Vet", "Firulais",
                    "Sede Norte", "Calle 1 #2-3", "Notas");

            Map<String, Object> variables = enviarYCapturarVariables(datos);

            assertThat(variables).containsEntry("COMPANY_NAME", "tu clínica");
        }
    }

    @Nested
    @DisplayName("variables de la plantilla con datos completos")
    class VariablesConDatosCompletos {

        @Test
        @DisplayName("cada variable de la plantilla trae el dato resuelto")
        void cada_variable_trae_el_dato_resuelto() {
            Map<String, Object> variables = enviarYCapturarVariables(datosCompletos());

            assertThat(variables).containsEntry("RECIPIENT_NAME", "Ana Ruiz")
                    .containsEntry("RECIPIENT_EMAIL", "ana@example.com")
                    .containsEntry("COMPANY_NAME", "Clinica Norte")
                    .containsEntry("APPOINTMENT_DATE", "Jueves, 20 de agosto de 2026")
                    .containsEntry("APPOINTMENT_TIME", "09:30")
                    .containsEntry("APPOINTMENT_TYPE", "Consulta")
                    .containsEntry("VET_NAME", "Dra. Vet").containsEntry("PET_NAME", "Firulais")
                    .containsEntry("BRANCH_NAME", "Sede Norte")
                    .containsEntry("BRANCH_ADDRESS", "Calle 1 #2-3")
                    .containsEntry("NOTES", "Traer carnet de vacunas");
        }
    }

    @Nested
    @DisplayName("variables con datos ausentes: cada una cae a su valor por defecto")
    class VariablesConDatosAusentes {

        private static AppointmentConfirmationData datosMinimos() {
            return new AppointmentConfirmationData("walkin@example.com", null, null, INICIO,
                    AppointmentType.GROOMING, null, null, null, null, null);
        }

        @Test
        @DisplayName("nombre del destinatario ausente deja la variable vacia, no null")
        void nombre_del_destinatario_ausente_deja_variable_vacia() {
            Map<String, Object> variables = enviarYCapturarVariables(datosMinimos());

            assertThat(variables).containsEntry("RECIPIENT_NAME", "");
        }

        @Test
        @DisplayName("sin veterinario asignado cae en 'Por asignar'")
        void sin_veterinario_cae_en_por_asignar() {
            Map<String, Object> variables = enviarYCapturarVariables(datosMinimos());

            assertThat(variables).containsEntry("VET_NAME", "Por asignar");
        }

        @Test
        @DisplayName("sin mascota registrada cae en el guion largo")
        void sin_mascota_cae_en_guion() {
            Map<String, Object> variables = enviarYCapturarVariables(datosMinimos());

            assertThat(variables).containsEntry("PET_NAME", "—");
        }

        @Test
        @DisplayName("sin sede resuelta el nombre y la direccion caen en el guion largo")
        void sin_sede_cae_en_guion() {
            Map<String, Object> variables = enviarYCapturarVariables(datosMinimos());

            assertThat(variables).containsEntry("BRANCH_NAME", "—").containsEntry("BRANCH_ADDRESS",
                    "—");
        }

        @Test
        @DisplayName("sin notas cae en el mensaje por defecto")
        void sin_notas_cae_en_mensaje_por_defecto() {
            Map<String, Object> variables = enviarYCapturarVariables(datosMinimos());

            assertThat(variables).containsEntry("NOTES", "Sin observaciones.");
        }
    }

    @Nested
    @DisplayName("etiqueta del tipo de cita — recorre el enum completo")
    class EtiquetaDelTipo {

        static Stream<Arguments> tiposYEtiquetas() {
            return Stream.of(arguments(AppointmentType.CONSULTATION, "Consulta"),
                    arguments(AppointmentType.CONTROL, "Control"),
                    arguments(AppointmentType.VACCINATION, "Vacunación"),
                    arguments(AppointmentType.DEWORMING, "Desparasitación"),
                    arguments(AppointmentType.SURGERY, "Cirugía"),
                    arguments(AppointmentType.IMAGING, "Imagenología"),
                    arguments(AppointmentType.LABORATORY, "Laboratorio"),
                    arguments(AppointmentType.GROOMING, "Estética"),
                    arguments(AppointmentType.OTHER, "Otro"));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @MethodSource("tiposYEtiquetas")
        @DisplayName("cada tipo de cita mapea a su etiqueta en castellano")
        void cada_tipo_mapea_a_su_etiqueta(AppointmentType tipo, String etiquetaEsperada) {
            AppointmentConfirmationData datos = new AppointmentConfirmationData("ana@example.com",
                    "Ana Ruiz", "Clinica Norte", INICIO, tipo, "Dra. Vet", "Firulais", "Sede Norte",
                    "Calle 1 #2-3", "Notas");

            Map<String, Object> variables = enviarYCapturarVariables(datos);

            assertThat(variables).containsEntry("APPOINTMENT_TYPE", etiquetaEsperada);
        }

        @Test
        @DisplayName("un tipo nulo cae en la etiqueta generica 'Cita'")
        void tipo_nulo_cae_en_etiqueta_generica() {
            AppointmentConfirmationData datos = new AppointmentConfirmationData("ana@example.com",
                    "Ana Ruiz", "Clinica Norte", INICIO, null, "Dra. Vet", "Firulais", "Sede Norte",
                    "Calle 1 #2-3", "Notas");

            Map<String, Object> variables = enviarYCapturarVariables(datos);

            assertThat(variables).containsEntry("APPOINTMENT_TYPE", "Cita");
        }
    }

    /**
     * Sin el default commiteado en application.yml, una clave ausente ya no degrada
     * a "correo que no sale": tumba el arranque. Este correo es el unico acuse de
     * que la cita existe, asi que perderlo produce una agenda con una cita a la que
     * nadie se presenta.
     */
    @Nested
    @DisplayName("configuracion incompleta")
    class ConfiguracionIncompleta {

        private ResendAppointmentConfirmationEmailSender sinPlantilla() {
            return new ResendAppointmentConfirmationEmailSender(email, "");
        }

        @Test
        @DisplayName("con el correo habilitado, la plantilla vacia TUMBA el arranque")
        void la_plantilla_vacia_tumba_el_arranque() {
            when(email.isEnabled()).thenReturn(true);

            assertThatThrownBy(this::sinPlantilla).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("confirmation-template-id");
        }

        @Test
        @DisplayName("con el correo deshabilitado no exige nada: es el modo de las rodajas y del contrato OpenAPI")
        void con_el_correo_deshabilitado_no_exige_nada() {
            when(email.isEnabled()).thenReturn(false);

            assertThatCode(this::sinPlantilla).doesNotThrowAnyException();
        }
    }
}
