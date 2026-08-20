package com.vetsoftware.app.medicationschedule.infrastructure.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.command.GenerateMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.command.RescheduleMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.dto.RescheduleResultDto;
import com.vetsoftware.app.medicationschedule.application.port.in.ApplyMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.GenerateMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.ListMedicationSchedulesByHospitalizationUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.RescheduleMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.SuspendPendingMedicationSchedulesUseCase;
import com.vetsoftware.app.medicationschedule.domain.CascadeSkipReason;
import com.vetsoftware.app.medicationschedule.domain.RescheduleMode;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller del plan de tomas: rutas, binding, validacion del
 * request y forma del JSON. Los cinco casos de uso son dobles.
 *
 * <p>
 * Ningun request trae {@code companyId} —eso seria suplantable—: apply y
 * reschedule lo inyecta el controller con {@code currentCompanyIdOrNull()},
 * porque la toma no tiene empresa propia y el caso de uso necesita subir a la
 * orden para validar la propiedad. La autoria del plan la sella
 * {@code Authz.currentEmployeeId()}.
 */
@WebMvcTest(MedicationScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("MedicationScheduleController — contrato HTTP")
class MedicationScheduleControllerTest {

    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long MEDICATION_ID = 300L;
    private static final Long HOSPITALIZATION_ID = 400L;
    private static final Long SCHEDULE_ID = 500L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private GenerateMedicationScheduleUseCase generateUseCase;
    @MockitoBean
    private ListMedicationSchedulesByHospitalizationUseCase listByHospitalizationUseCase;
    @MockitoBean
    private ApplyMedicationScheduleUseCase applyUseCase;
    @MockitoBean
    private RescheduleMedicationScheduleUseCase rescheduleUseCase;
    @MockitoBean
    private SuspendPendingMedicationSchedulesUseCase suspendPendingUseCase;

    /**
     * {@code generate} sella el autor con {@code currentEmployeeId()}, distinto del
     * {@code currentEmployeeIdOrNull()} que ya stubea {@link WebMvcSliceConfig}.
     * Sin este stub Mockito devolveria null y el command llegaria sin empleado.
     *
     * <p>
     * {@code currentCompanyIdOrNull()} tampoco lo stubea la configuracion comun y
     * es lo que apply/reschedule meten en su command: sin el, el command viajaria
     * con {@code companyId} nulo, que es el camino SYSTEM y no el de un empleado.
     */
    @BeforeEach
    void resolverElContexto() {
        when(authz.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
        when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
    }

    private static MedicationScheduleDto toma() {
        return new MedicationScheduleDto(SCHEDULE_ID, MEDICATION_ID, "Amoxicilina 500mg",
                LocalDateTime.of(2026, 1, 15, 8, 0), LocalDateTime.of(2026, 1, 15, 8, 0), null,
                "PENDING", false, EMPLOYEE_ID, "EMP-001", "Ana Ruiz",
                LocalDateTime.of(2026, 1, 15, 7, 30), true);
    }

    @Nested
    @DisplayName("generacion del calendario")
    class Generacion {

        @Test
        @DisplayName("POST /medication-schedules/generate/{id} responde 201 y sella el empleado autor")
        void post_generate_responde_201_y_sella_el_empleado() throws Exception {
            when(generateUseCase.execute(any())).thenReturn(List.of(toma()));

            mockMvc.perform(post("/medication-schedules/generate/300"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$[0].id").value(500))
                    .andExpect(jsonPath("$[0].hospitalizationMedication.id").value(300))
                    .andExpect(jsonPath("$[0].hospitalizationMedication.name")
                            .value("Amoxicilina 500mg"))
                    .andExpect(jsonPath("$[0].appliedStatus").value("PENDING"))
                    .andExpect(jsonPath("$[0].createdBy.id").value(EMPLOYEE_ID))
                    .andExpect(jsonPath("$[0].createdBy.employeeCode").value("EMP-001"))
                    .andExpect(jsonPath("$[0].createdBy.name").value("Ana Ruiz"));

            verify(generateUseCase)
                    .execute(new GenerateMedicationScheduleCommand(300L, EMPLOYEE_ID, COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("lectura del plan")
    class Lecturas {

        @Test
        @DisplayName("GET /medication-schedules/by-hospitalization/{id} devuelve el plan completo")
        void get_by_hospitalization_devuelve_el_plan() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(HOSPITALIZATION_ID, COMPANY_ID))
                    .thenReturn(List.of(toma()));

            mockMvc.perform(get("/medication-schedules/by-hospitalization/400"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(500))
                    .andExpect(jsonPath("$[0].currentDateTime").value("2026-01-15T08:00:00"));
        }
    }

    @Nested
    @DisplayName("aplicacion de una toma")
    class Aplicacion {

        @Test
        @DisplayName("PATCH /medication-schedules/{id}/apply sella la empresa del contexto")
        void patch_apply_marca_la_toma_y_devuelve_el_plan() throws Exception {
            when(applyUseCase.execute(new ApplyMedicationScheduleCommand(SCHEDULE_ID, COMPANY_ID)))
                    .thenReturn(List.of(toma()));

            mockMvc.perform(patch("/medication-schedules/500/apply")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(500));

            verify(applyUseCase)
                    .execute(new ApplyMedicationScheduleCommand(SCHEDULE_ID, COMPANY_ID));
        }
    }

    /**
     * El {@code mode} era {@code String} y llegaba crudo al caso de uso pese al
     * {@code @Valid}: un valor mal escrito degradaba a «solo esta toma» y devolvia
     * 200 (#134). Ahora es enum, asi que lo desconocido lo corta el deserializador
     * —400 {@code MALFORMED_REQUEST}, sin tocar el caso de uso— y el desenlace de
     * la cascada viaja en el cuerpo de la respuesta.
     */
    @Nested
    @DisplayName("reprogramacion de una toma")
    class Reprogramacion {

        private static final String BODY = """
                {"newDateTime":"2026-01-16T08:00:00","mode":"%s"}
                """;

        /**
         * Los dos fronts desplegados mandan el modo en minusculas —la comparacion vieja
         * era {@code equalsIgnoreCase}— y eso es lo que sostiene el
         * {@code ACCEPT_CASE_INSENSITIVE_VALUES} del request: sin el, cada arrastre de
         * dosis desde un front vivo pasaria de 200 a 400.
         */
        @ParameterizedTest(name = "mode={0} llega como {1}")
        @CsvSource({"one, ONE", "cascade, CASCADE", "ONE, ONE", "CASCADE, CASCADE"})
        @DisplayName("PATCH .../reschedule traduce el body al command sin importar la caja del modo")
        void patch_reschedule_traduce_el_body_al_command(String enviado, RescheduleMode esperado)
                throws Exception {
            when(rescheduleUseCase.execute(any()))
                    .thenReturn(RescheduleResultDto.notCascaded(List.of(toma())));

            mockMvc.perform(patch("/medication-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted(enviado)))
                    .andExpect(status().isOk());

            verify(rescheduleUseCase).execute(new RescheduleMedicationScheduleCommand(SCHEDULE_ID,
                    LocalDateTime.of(2026, 1, 16, 8, 0), esperado, COMPANY_ID));
        }

        @ParameterizedTest(name = "mode={0}")
        @ValueSource(strings = {"cascada", "todas", "ALL", "one cascade"})
        @DisplayName("PATCH .../reschedule con un modo desconocido responde 400 y no llega al caso de uso")
        void patch_reschedule_con_modo_desconocido_responde_400(String modo) throws Exception {
            mockMvc.perform(patch("/medication-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted(modo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

            // Lo importante no es el 400 sino que no se reprograme nada: antes esto
            // movia la toma con el alcance equivocado y respondia 200.
            verify(rescheduleUseCase, never()).execute(any());
        }

        /**
         * Deshabilitado porque hoy falla: Jackson resuelve una cadena numerica como el
         * {@code ordinal()} del enum, asi que {@code "0"} entra como ONE y {@code "1"}
         * como CASCADE —el alcance lo decide la posicion de declaracion, que nadie
         * eligio como contrato—. Es la misma clase de fallo que #134 por otra puerta.
         * Al arreglarlo, quitar el {@code @Disabled}.
         *
         * @see <a href=
         *      "https://github.com/kefaroTech/vetsoftware-backend/issues/228">#228</a>
         */
        @Disabled("#228: mode=\"1\" se deserializa como el ordinal CASCADE en vez de rechazarse"
                + " — https://github.com/kefaroTech/vetsoftware-backend/issues/228")
        @ParameterizedTest(name = "mode={0}")
        @ValueSource(strings = {"0", "1"})
        @DisplayName("PATCH .../reschedule con el modo por indice responde 400 y no llega al caso de uso")
        void patch_reschedule_con_modo_por_indice_responde_400(String modo) throws Exception {
            mockMvc.perform(patch("/medication-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted(modo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

            verify(rescheduleUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PATCH /medication-schedules/{id}/reschedule sin nueva fecha responde 400")
        void patch_reschedule_sin_fecha_responde_400() throws Exception {
            mockMvc.perform(patch("/medication-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mode":"one"}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            verify(rescheduleUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PATCH /medication-schedules/{id}/reschedule sin modo responde 400")
        void patch_reschedule_sin_modo_responde_400() throws Exception {
            mockMvc.perform(patch("/medication-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"newDateTime":"2026-01-16T08:00:00"}
                            """)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

            verify(rescheduleUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una cascada aplicada devuelve el plan con cascadeApplied=true y sin motivo")
        void patch_reschedule_con_cascada_aplicada_devuelve_el_desenlace() throws Exception {
            when(rescheduleUseCase.execute(any()))
                    .thenReturn(RescheduleResultDto.applied(List.of(toma())));

            mockMvc.perform(patch("/medication-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted("cascade")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.schedules[0].id").value(500))
                    .andExpect(
                            jsonPath("$.schedules[0].currentDateTime").value("2026-01-15T08:00:00"))
                    .andExpect(jsonPath("$.cascadeApplied").value(true))
                    .andExpect(jsonPath("$.cascadeSkippedReason").value(nullValue()));
        }

        /**
         * Recorre los tres motivos: si aparece uno nuevo en el enum y no se serializa,
         * el front se queda sin saber por que no se movio la pauta —que es justo el
         * agujero de #134.
         */
        @ParameterizedTest(name = "motivo {0}")
        @EnumSource(CascadeSkipReason.class)
        @DisplayName("una cascada pedida y no aplicada devuelve cascadeApplied=false y su motivo")
        void patch_reschedule_con_cascada_saltada_devuelve_el_motivo(CascadeSkipReason motivo)
                throws Exception {
            when(rescheduleUseCase.execute(any()))
                    .thenReturn(RescheduleResultDto.skipped(List.of(toma()), motivo));

            mockMvc.perform(patch("/medication-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content(BODY.formatted("cascade")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.schedules[0].id").value(500))
                    .andExpect(jsonPath("$.cascadeApplied").value(false))
                    .andExpect(jsonPath("$.cascadeSkippedReason").value(motivo.name()));
        }
    }

    @Nested
    @DisplayName("suspension de las tomas pendientes")
    class Suspension {

        @Test
        @DisplayName("PATCH .../suspend-pending devuelve solo las tomas que quedan aplicadas")
        void patch_suspend_pending_devuelve_las_aplicadas() throws Exception {
            when(suspendPendingUseCase.execute(MEDICATION_ID, COMPANY_ID))
                    .thenReturn(List.of(toma()));

            mockMvc.perform(patch("/medication-schedules/by-medication/300/suspend-pending"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(500));
        }
    }
}
