package com.vetsoftware.app.procedureschedule.infrastructure.web;

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
import com.vetsoftware.app.procedureschedule.application.command.ApplyProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.command.GenerateProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.command.RescheduleProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.ApplyProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.in.GenerateProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.in.ListProcedureSchedulesByHospitalizationUseCase;
import com.vetsoftware.app.procedureschedule.application.port.in.RescheduleProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.in.SuspendPendingProcedureSchedulesUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
 * porque la ejecucion no tiene empresa propia y el caso de uso necesita subir a
 * la orden para validar la propiedad. La autoria del plan la sella
 * {@code Authz.currentEmployeeId()}.
 */
@WebMvcTest(ProcedureScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ProcedureScheduleController — contrato HTTP")
class ProcedureScheduleControllerTest {

    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long PROCEDURE_ID = 300L;
    private static final Long HOSPITALIZATION_ID = 400L;
    private static final Long SCHEDULE_ID = 500L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private GenerateProcedureScheduleUseCase generateUseCase;
    @MockitoBean
    private ListProcedureSchedulesByHospitalizationUseCase listByHospitalizationUseCase;
    @MockitoBean
    private ApplyProcedureScheduleUseCase applyUseCase;
    @MockitoBean
    private RescheduleProcedureScheduleUseCase rescheduleUseCase;
    @MockitoBean
    private SuspendPendingProcedureSchedulesUseCase suspendPendingUseCase;

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

    private static ProcedureScheduleDto toma() {
        return new ProcedureScheduleDto(SCHEDULE_ID, PROCEDURE_ID, "Curacion de herida",
                LocalDateTime.of(2026, 1, 15, 8, 0), LocalDateTime.of(2026, 1, 15, 8, 0), null,
                "PENDING", false, EMPLOYEE_ID, "EMP-001", "Ana Ruiz",
                LocalDateTime.of(2026, 1, 15, 7, 30), true);
    }

    @Nested
    @DisplayName("generacion del calendario")
    class Generacion {

        @Test
        @DisplayName("POST /procedure-schedules/generate/{id} responde 201 y sella el empleado autor")
        void post_generate_responde_201_y_sella_el_empleado() throws Exception {
            when(generateUseCase.execute(any())).thenReturn(List.of(toma()));

            mockMvc.perform(post("/procedure-schedules/generate/300"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$[0].id").value(500))
                    .andExpect(jsonPath("$[0].hospitalizationProcedure.id").value(300))
                    .andExpect(jsonPath("$[0].hospitalizationProcedure.name")
                            .value("Curacion de herida"))
                    .andExpect(jsonPath("$[0].appliedStatus").value("PENDING"))
                    .andExpect(jsonPath("$[0].createdBy.id").value(EMPLOYEE_ID))
                    .andExpect(jsonPath("$[0].createdBy.employeeCode").value("EMP-001"))
                    .andExpect(jsonPath("$[0].createdBy.name").value("Ana Ruiz"));

            verify(generateUseCase)
                    .execute(new GenerateProcedureScheduleCommand(300L, EMPLOYEE_ID, COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("lectura del plan")
    class Lecturas {

        @Test
        @DisplayName("GET /procedure-schedules/by-hospitalization/{id} devuelve el plan completo")
        void get_by_hospitalization_devuelve_el_plan() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(HOSPITALIZATION_ID, COMPANY_ID))
                    .thenReturn(List.of(toma()));

            mockMvc.perform(get("/procedure-schedules/by-hospitalization/400"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(500))
                    .andExpect(jsonPath("$[0].currentDateTime").value("2026-01-15T08:00:00"));
        }
    }

    @Nested
    @DisplayName("aplicacion de una toma")
    class Aplicacion {

        @Test
        @DisplayName("PATCH /procedure-schedules/{id}/apply sella la empresa del contexto")
        void patch_apply_marca_la_toma_y_devuelve_el_plan() throws Exception {
            when(applyUseCase.execute(new ApplyProcedureScheduleCommand(SCHEDULE_ID, COMPANY_ID)))
                    .thenReturn(List.of(toma()));

            mockMvc.perform(patch("/procedure-schedules/500/apply")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(500));

            verify(applyUseCase)
                    .execute(new ApplyProcedureScheduleCommand(SCHEDULE_ID, COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("reprogramacion de una toma")
    class Reprogramacion {

        @Test
        @DisplayName("PATCH /procedure-schedules/{id}/reschedule traduce el body al command")
        void patch_reschedule_traduce_el_body_al_command() throws Exception {
            when(rescheduleUseCase.execute(any())).thenReturn(List.of(toma()));

            mockMvc.perform(patch("/procedure-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"newDateTime":"2026-01-16T08:00:00","mode":"cascade"}
                            """)).andExpect(status().isOk());

            verify(rescheduleUseCase).execute(new RescheduleProcedureScheduleCommand(SCHEDULE_ID,
                    LocalDateTime.of(2026, 1, 16, 8, 0), "cascade", COMPANY_ID));
        }

        @Test
        @DisplayName("PATCH /procedure-schedules/{id}/reschedule sin nueva fecha responde 400")
        void patch_reschedule_sin_fecha_responde_400() throws Exception {
            mockMvc.perform(patch("/procedure-schedules/500/reschedule")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mode":"one"}
                            """)).andExpect(status().isBadRequest());

            verify(rescheduleUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("suspension de las tomas pendientes")
    class Suspension {

        @Test
        @DisplayName("PATCH .../suspend-pending devuelve solo las tomas que quedan aplicadas")
        void patch_suspend_pending_devuelve_las_aplicadas() throws Exception {
            when(suspendPendingUseCase.execute(PROCEDURE_ID, COMPANY_ID))
                    .thenReturn(List.of(toma()));

            mockMvc.perform(patch("/procedure-schedules/by-procedure/300/suspend-pending"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(500));
        }
    }
}
