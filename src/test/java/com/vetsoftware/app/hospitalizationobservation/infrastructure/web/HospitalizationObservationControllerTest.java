package com.vetsoftware.app.hospitalizationobservation.infrastructure.web;

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
import com.vetsoftware.app.hospitalizationobservation.application.command.CreateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.command.UpdateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.CreateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.DeleteHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.FindHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ListHospitalizationObservationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ReactivateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.UpdateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
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
 * Rodaja HTTP del controller: rutas, binding, codigos de estado y forma del
 * JSON. Los casos de uso van mockeados — aqui no se prueba la logica de
 * negocio.
 */
@WebMvcTest(HospitalizationObservationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("HospitalizationObservationController — contrato HTTP")
class HospitalizationObservationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateHospitalizationObservationUseCase createUseCase;
    @MockitoBean
    private UpdateHospitalizationObservationUseCase updateUseCase;
    @MockitoBean
    private FindHospitalizationObservationUseCase findUseCase;
    @MockitoBean
    private ListHospitalizationObservationsByHospitalizationUseCase listByHospitalizationUseCase;
    @MockitoBean
    private DeleteHospitalizationObservationUseCase deleteUseCase;
    @MockitoBean
    private ReactivateHospitalizationObservationUseCase reactivateUseCase;

    /**
     * El controller sella la autoria con {@code authz.currentEmployeeId()},
     * distinto del {@code *OrNull()} que ya stubea {@link WebMvcSliceConfig}: sin
     * este stub Mockito devolveria 0L para un {@code long} y el command llegaria
     * firmado por un empleado que no existe.
     */
    @BeforeEach
    void resolverElEmpleadoDelContexto() {
        when(authz.currentEmployeeId()).thenReturn(WebMvcSliceConfig.EMPLOYEE_ID);
    }

    private static HospitalizationObservationDto observacion() {
        return new HospitalizationObservationDto(800L, "Paciente estable, sin novedades",
                new HospitalizationSummaryDto(600L, LocalDate.of(2026, 3, 1)),
                new EmployeeSummaryDto(4L, "EMP-001", "Ana Ruiz"),
                LocalDateTime.of(2026, 3, 1, 10, 30), true);
    }

    @Nested
    @DisplayName("POST /hospitalization-observations")
    class Create {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(observacion());

            mockMvc.perform(post("/hospitalization-observations")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"description":"Paciente estable, sin novedades","hospitalizationId":600}
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(800))
                    .andExpect(jsonPath("$.hospitalization.id").value(600))
                    .andExpect(jsonPath("$.createdBy.name").value("Ana Ruiz"));
        }

        @Test
        @DisplayName("traduce el request al command con el empleado del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(observacion());

            mockMvc.perform(post("/hospitalization-observations")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"description":"Paciente estable, sin novedades","hospitalizationId":600}
                                    """));

            verify(createUseCase).execute(new CreateHospitalizationObservationCommand(
                    "Paciente estable, sin novedades", 600L, WebMvcSliceConfig.EMPLOYEE_ID));
        }

        @Test
        @DisplayName("sin description responde 400 y no llega al caso de uso")
        void sin_description_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-observations")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"hospitalizationId\":600}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /hospitalization-observations/by-hospitalization/{hospitalizationId}")
    class ListarPorHospitalizacion {

        @Test
        @DisplayName("pagina por la empresa del contexto")
        void pagina_por_la_empresa_del_contexto() throws Exception {
            when(authz.currentCompanyId()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(listByHospitalizationUseCase.listByHospitalization(600L,
                    WebMvcSliceConfig.COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(observacion()), 0, 20, 1, 1));

            mockMvc.perform(get("/hospitalization-observations/by-hospitalization/600"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(800));
        }
    }

    @Nested
    @DisplayName("GET /hospitalization-observations/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("responde 200 con el recurso encontrado")
        void responde_200() throws Exception {
            when(authz.currentCompanyId()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(findUseCase.findById(800L, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(observacion());

            mockMvc.perform(get("/hospitalization-observations/800")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Paciente estable, sin novedades"));
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(authz.currentCompanyId()).thenReturn(WebMvcSliceConfig.COMPANY_ID);
            when(findUseCase.findById(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new HospitalizationObservationNotFoundException(99L));

            mockMvc.perform(get("/hospitalization-observations/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /hospitalization-observations/{id}")
    class Update {

        @Test
        @DisplayName("responde 200 y traduce el request al command")
        void responde_200_y_traduce_el_request() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(observacion());

            mockMvc.perform(
                    put("/hospitalization-observations/800").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\":\"Nueva evolucion\"}"))
                    .andExpect(status().isOk());

            // La empresa la pone el controller desde el contexto: nunca viaja en el body.
            verify(updateUseCase).execute(new UpdateHospitalizationObservationCommand(800L,
                    "Nueva evolucion", WebMvcSliceConfig.COMPANY_ID));
        }

        @Test
        @DisplayName("sin description responde 400 y no llega al caso de uso")
        void sin_description_responde_400() throws Exception {
            mockMvc.perform(put("/hospitalization-observations/800")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("DELETE /hospitalization-observations/{id}")
    class Delete {

        @Test
        @DisplayName("responde 204 sin cuerpo y delega en el caso de uso")
        void responde_204_sin_cuerpo() throws Exception {
            mockMvc.perform(delete("/hospitalization-observations/800"))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(800L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new HospitalizationObservationNotFoundException(99L))
                    .when(deleteUseCase).execute(99L, WebMvcSliceConfig.COMPANY_ID);

            mockMvc.perform(delete("/hospitalization-observations/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /hospitalization-observations/{id}/enable")
    class Reactivate {

        @Test
        @DisplayName("responde 200 con la observacion reactivada")
        void responde_200_con_la_observacion_reactivada() throws Exception {
            when(reactivateUseCase.execute(800L, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(observacion());

            mockMvc.perform(patch("/hospitalization-observations/800/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(800L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new HospitalizationObservationNotFoundException(99L));

            mockMvc.perform(patch("/hospitalization-observations/99/enable"))
                    .andExpect(status().isNotFound());
        }
    }
}
