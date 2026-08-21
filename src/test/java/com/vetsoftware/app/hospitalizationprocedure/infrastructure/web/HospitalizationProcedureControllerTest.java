package com.vetsoftware.app.hospitalizationprocedure.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalizationprocedure.application.command.CreateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.command.SuspendHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.command.UpdateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.CreateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.DeleteHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.FindHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ListHospitalizationProceduresByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ReactivateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.SuspendHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.UpdateHospitalizationProcedureUseCase;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
 * Rodaja HTTP del controller de ordenes de procedimiento: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 *
 * <p>
 * La unica rama que el JaCoCo de hoy marca en este controller es el ternario de
 * {@code toResponse} para {@code suspensionBy}:
 * {@link Estado#patch_suspend_sella_el_empleado_del_contexto()} cubre el lado
 * no-null y el resto de los tests cubren el lado null.
 */
@WebMvcTest(HospitalizationProcedureController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("HospitalizationProcedureController — contrato HTTP")
class HospitalizationProcedureControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long OTHER_EMPLOYEE_ID = 5L;
    private static final Long PROCEDURE_ID = 500L;
    private static final Long HOSPITALIZATION_ID = 20L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateHospitalizationProcedureUseCase createUseCase;
    @MockitoBean
    private UpdateHospitalizationProcedureUseCase updateUseCase;
    @MockitoBean
    private FindHospitalizationProcedureUseCase findUseCase;
    @MockitoBean
    private ListHospitalizationProceduresByHospitalizationUseCase listByHospitalizationUseCase;
    @MockitoBean
    private DeleteHospitalizationProcedureUseCase deleteUseCase;
    @MockitoBean
    private ReactivateHospitalizationProcedureUseCase reactivateUseCase;
    @MockitoBean
    private SuspendHospitalizationProcedureUseCase suspendUseCase;

    /**
     * El controller usa {@code authz.currentEmployeeId()}, distinto del
     * {@code *OrNull()} que ya stubea {@link WebMvcSliceConfig}: sin este stub
     * Mockito devolveria 0L —no null— y el comando llegaria firmado por un empleado
     * que no existe.
     */
    @BeforeEach
    void resolverElEmpleadoDelContexto() {
        when(authz.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
    }

    private static HospitalizationProcedureDto dto() {
        return new HospitalizationProcedureDto(PROCEDURE_ID, "Curacion de herida",
                "Solucion salina 0.9%", "EVERY_8H", "INTERVAL", "DAYS", 5, LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), "Notas",
                new HospitalizationSummaryDto(HOSPITALIZATION_ID, LocalDate.of(2026, 3, 1)),
                new EmployeeSummaryDto(EMPLOYEE_ID, "EMP-001", "Ana Ruiz"),
                LocalDateTime.of(2026, 3, 1, 8, 0), true, null, null);
    }

    private static HospitalizationProcedureDto dtoSuspendida() {
        return new HospitalizationProcedureDto(PROCEDURE_ID, "Curacion de herida",
                "Solucion salina 0.9%", "EVERY_8H", "INTERVAL", "DAYS", 5, LocalDate.of(2026, 3, 1),
                LocalTime.of(8, 0), "Notas",
                new HospitalizationSummaryDto(HOSPITALIZATION_ID, LocalDate.of(2026, 3, 1)),
                new EmployeeSummaryDto(EMPLOYEE_ID, "EMP-001", "Ana Ruiz"),
                LocalDateTime.of(2026, 3, 1, 8, 0), true, LocalDateTime.of(2026, 3, 2, 9, 0),
                new EmployeeSummaryDto(OTHER_EMPLOYEE_ID, "EMP-002", "Luis Paz"));
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("POST crea y sella el empleado del contexto como creador")
        void post_crea_y_sella_el_empleado_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/hospitalization-procedures")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Curacion de herida","dose":"Solucion salina 0.9%",
                                     "frequency":"EVERY_8H","guidelineType":"INTERVAL","durationMeasure":"DAYS",
                                     "durationQuantity":5,"startDate":"2026-03-01","startTime":"08:00",
                                     "notes":"Notas","hospitalizationId":20}
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(PROCEDURE_ID))
                    .andExpect(jsonPath("$.hospitalization.id").value(HOSPITALIZATION_ID))
                    .andExpect(jsonPath("$.createdBy.employeeCode").value("EMP-001"))
                    .andExpect(jsonPath("$.suspensionBy").doesNotExist());

            // Ni el createdById ni el companyId viajan en el request: los pone el
            // backend desde el AuthContext. El JSON de arriba no los trae, y el command
            // esperado si.
            verify(createUseCase).execute(new CreateHospitalizationProcedureCommand(
                    "Curacion de herida", "Solucion salina 0.9%", "EVERY_8H", "INTERVAL", "DAYS", 5,
                    LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas", HOSPITALIZATION_ID,
                    EMPLOYEE_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("POST sin nombre responde 400 y no llega al caso de uso")
        void post_sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-procedures")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"hospitalizationId":20}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST sin hospitalizationId responde 400")
        void post_sin_hospitalization_id_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-procedures")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Curacion de herida"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("lectura")
    class Lectura {

        @Test
        @DisplayName("GET por id devuelve el procedimiento acotado por la empresa del contexto")
        void get_por_id() throws Exception {
            when(findUseCase.findById(PROCEDURE_ID, COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(get("/hospitalization-procedures/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Curacion de herida"));
        }

        @Test
        @DisplayName("GET por hospitalizacion usa los defectos de paginacion")
        void get_por_hospitalizacion_usa_los_defectos() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(HOSPITALIZATION_ID, COMPANY_ID,
                    0, 20)).thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/hospitalization-procedures/by-hospitalization/20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(PROCEDURE_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET por hospitalizacion traslada page y pageSize explicitos")
        void get_por_hospitalizacion_traslada_paginacion_explicita() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(HOSPITALIZATION_ID, COMPANY_ID,
                    2, 5)).thenReturn(PageResult.empty(2, 5));

            mockMvc.perform(get("/hospitalization-procedures/by-hospitalization/20")
                    .param("page", "2").param("pageSize", "5")).andExpect(status().isOk());

            verify(listByHospitalizationUseCase).listByHospitalization(HOSPITALIZATION_ID,
                    COMPANY_ID, 2, 5);
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("PUT actualiza y devuelve el procedimiento con los nuevos valores")
        void put_actualiza() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/hospitalization-procedures/500")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Curacion de herida","dose":"Solucion salina 0.9%",
                                     "frequency":"EVERY_8H","guidelineType":"INTERVAL","durationMeasure":"DAYS",
                                     "durationQuantity":5,"startDate":"2026-03-01","startTime":"08:00",
                                     "notes":"Notas"}
                                    """))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(PROCEDURE_ID));

            // El companyId del comando no viene del JSON: lo pone el controller desde
            // el contexto. Si lo tomara del request o lo dejara null, este verify cae.
            verify(updateUseCase).execute(new UpdateHospitalizationProcedureCommand(PROCEDURE_ID,
                    "Curacion de herida", "Solucion salina 0.9%", "EVERY_8H", "INTERVAL", "DAYS", 5,
                    LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas", COMPANY_ID));
        }

        @Test
        @DisplayName("PUT sin nombre responde 400")
        void put_sin_nombre_responde_400() throws Exception {
            mockMvc.perform(put("/hospitalization-procedures/500")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("DELETE responde 204 y delega acotando por la empresa del contexto")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/hospitalization-procedures/500"))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(PROCEDURE_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("reactivacion y suspension")
    class Estado {

        @Test
        @DisplayName("PATCH enable reactiva acotando por la empresa del contexto")
        void patch_enable_reactiva() throws Exception {
            // El stub esta atado al par (id, empresa del contexto): si el controller
            // dejara de pasar el companyId, devolveria null y el test cae.
            when(reactivateUseCase.execute(PROCEDURE_ID, COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(patch("/hospitalization-procedures/500/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(PROCEDURE_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH suspend sella el empleado del contexto y devuelve quien suspendio")
        void patch_suspend_sella_el_empleado_del_contexto() throws Exception {
            when(suspendUseCase.execute(any())).thenReturn(dtoSuspendida());

            mockMvc.perform(patch("/hospitalization-procedures/500/suspend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suspensionBy.employeeCode").value("EMP-002"))
                    .andExpect(jsonPath("$.suspensionDate").exists());

            verify(suspendUseCase).execute(new SuspendHospitalizationProcedureCommand(PROCEDURE_ID,
                    EMPLOYEE_ID, COMPANY_ID));
        }
    }
}
