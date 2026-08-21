package com.vetsoftware.app.hospitalizationmedication.infrastructure.web;

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
import com.vetsoftware.app.hospitalizationmedication.application.command.CreateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.command.SuspendHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.command.UpdateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.CreateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.DeleteHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.FindHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.ListHospitalizationMedicationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.ReactivateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.SuspendHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.UpdateHospitalizationMedicationUseCase;
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
 * Rodaja HTTP del controller de ordenes de medicacion: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 */
@WebMvcTest(HospitalizationMedicationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("HospitalizationMedicationController — contrato HTTP")
class HospitalizationMedicationControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long OTHER_EMPLOYEE_ID = 5L;
    private static final Long MEDICATION_ID = 500L;
    private static final Long HOSPITALIZATION_ID = 20L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateHospitalizationMedicationUseCase createUseCase;
    @MockitoBean
    private UpdateHospitalizationMedicationUseCase updateUseCase;
    @MockitoBean
    private FindHospitalizationMedicationUseCase findUseCase;
    @MockitoBean
    private ListHospitalizationMedicationsByHospitalizationUseCase listByHospitalizationUseCase;
    @MockitoBean
    private DeleteHospitalizationMedicationUseCase deleteUseCase;
    @MockitoBean
    private ReactivateHospitalizationMedicationUseCase reactivateUseCase;
    @MockitoBean
    private SuspendHospitalizationMedicationUseCase suspendUseCase;

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

    private static HospitalizationMedicationDto dto() {
        return new HospitalizationMedicationDto(MEDICATION_ID, "Amoxicilina 500mg", "1 tableta",
                "EVERY_8H", "INTERVAL", "DAYS", 5, LocalDate.of(2026, 3, 1), LocalTime.of(8, 0),
                "Notas",
                new HospitalizationSummaryDto(HOSPITALIZATION_ID, LocalDate.of(2026, 3, 1)),
                new EmployeeSummaryDto(EMPLOYEE_ID, "EMP-001", "Ana Ruiz"),
                LocalDateTime.of(2026, 3, 1, 8, 0), true, null, null);
    }

    private static HospitalizationMedicationDto dtoSuspendida() {
        return new HospitalizationMedicationDto(MEDICATION_ID, "Amoxicilina 500mg", "1 tableta",
                "EVERY_8H", "INTERVAL", "DAYS", 5, LocalDate.of(2026, 3, 1), LocalTime.of(8, 0),
                "Notas",
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

            mockMvc.perform(post("/hospitalization-medications")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Amoxicilina 500mg","dose":"1 tableta",
                                     "frequency":"EVERY_8H","guidelineType":"INTERVAL","durationMeasure":"DAYS",
                                     "durationQuantity":5,"startDate":"2026-03-01","startTime":"08:00",
                                     "notes":"Notas","hospitalizationId":20}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(MEDICATION_ID))
                    .andExpect(jsonPath("$.hospitalization.id").value(HOSPITALIZATION_ID))
                    .andExpect(jsonPath("$.createdBy.employeeCode").value("EMP-001"))
                    .andExpect(jsonPath("$.suspensionBy").doesNotExist());

            // Ni el createdById ni el companyId viajan en el request: los pone el
            // backend desde el AuthContext. El JSON de arriba no los trae, y el command
            // esperado si.
            verify(createUseCase).execute(new CreateHospitalizationMedicationCommand(
                    "Amoxicilina 500mg", "1 tableta", "EVERY_8H", "INTERVAL", "DAYS", 5,
                    LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas", HOSPITALIZATION_ID,
                    EMPLOYEE_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("POST sin nombre responde 400 y no llega al caso de uso")
        void post_sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-medications")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"hospitalizationId":20}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST sin hospitalizationId responde 400")
        void post_sin_hospitalization_id_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-medications")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Amoxicilina 500mg"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("lectura")
    class Lectura {

        @Test
        @DisplayName("GET por id devuelve la medicacion acotada por la empresa del contexto")
        void get_por_id() throws Exception {
            when(findUseCase.findById(MEDICATION_ID, COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(get("/hospitalization-medications/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Amoxicilina 500mg"));
        }

        @Test
        @DisplayName("GET por hospitalizacion usa los defectos de paginacion")
        void get_por_hospitalizacion_usa_los_defectos() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(HOSPITALIZATION_ID, COMPANY_ID,
                    0, 20)).thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/hospitalization-medications/by-hospitalization/20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(MEDICATION_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET por hospitalizacion traslada page y pageSize explicitos")
        void get_por_hospitalizacion_traslada_paginacion_explicita() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(HOSPITALIZATION_ID, COMPANY_ID,
                    2, 5)).thenReturn(PageResult.empty(2, 5));

            mockMvc.perform(get("/hospitalization-medications/by-hospitalization/20")
                    .param("page", "2").param("pageSize", "5")).andExpect(status().isOk());

            verify(listByHospitalizationUseCase).listByHospitalization(HOSPITALIZATION_ID,
                    COMPANY_ID, 2, 5);
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("PUT actualiza y devuelve la medicacion con los nuevos valores")
        void put_actualiza() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/hospitalization-medications/500")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"name":"Amoxicilina 500mg","dose":"1 tableta",
                                     "frequency":"EVERY_8H","guidelineType":"INTERVAL","durationMeasure":"DAYS",
                                     "durationQuantity":5,"startDate":"2026-03-01","startTime":"08:00",
                                     "notes":"Notas"}
                                    """))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(MEDICATION_ID));

            // La empresa la pone el controller desde el contexto: nunca viaja en el body.
            verify(updateUseCase).execute(new UpdateHospitalizationMedicationCommand(MEDICATION_ID,
                    "Amoxicilina 500mg", "1 tableta", "EVERY_8H", "INTERVAL", "DAYS", 5,
                    LocalDate.of(2026, 3, 1), LocalTime.of(8, 0), "Notas", COMPANY_ID));
        }

        @Test
        @DisplayName("PUT sin nombre responde 400")
        void put_sin_nombre_responde_400() throws Exception {
            mockMvc.perform(put("/hospitalization-medications/500")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("DELETE responde 204 y delega en el caso de uso")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/hospitalization-medications/500"))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(MEDICATION_ID, COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("reactivacion y suspension")
    class Estado {

        @Test
        @DisplayName("PATCH enable reactiva y devuelve la medicacion habilitada")
        void patch_enable_reactiva() throws Exception {
            when(reactivateUseCase.execute(MEDICATION_ID, COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(patch("/hospitalization-medications/500/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(MEDICATION_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH suspend sella el empleado del contexto y devuelve quien suspendio")
        void patch_suspend_sella_el_empleado_del_contexto() throws Exception {
            when(suspendUseCase.execute(any())).thenReturn(dtoSuspendida());

            mockMvc.perform(patch("/hospitalization-medications/500/suspend"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.suspensionBy.employeeCode").value("EMP-002"))
                    .andExpect(jsonPath("$.suspensionDate").exists());

            verify(suspendUseCase).execute(new SuspendHospitalizationMedicationCommand(
                    MEDICATION_ID, EMPLOYEE_ID, COMPANY_ID));
        }
    }
}
