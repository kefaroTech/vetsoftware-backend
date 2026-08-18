package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalizationprogressnote.application.command.CreateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.command.UpdateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.CreateHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.DeleteHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.FindHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.ListHospitalizationProgressNotesByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.ReactivateHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.UpdateHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. Lo que hay debajo son dobles.
 */
@WebMvcTest(HospitalizationProgressNoteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("HospitalizationProgressNoteController — contrato HTTP")
class HospitalizationProgressNoteControllerTest {

    private static final Long EMPLEADO_AUTENTICADO = 4L;
    private static final String CREAR_JSON = """
            {"description":"Paciente estable, buena respuesta al tratamiento","hospitalizationId":55}
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateHospitalizationProgressNoteUseCase createUseCase;
    @MockitoBean
    private UpdateHospitalizationProgressNoteUseCase updateUseCase;
    @MockitoBean
    private FindHospitalizationProgressNoteUseCase findUseCase;
    @MockitoBean
    private ListHospitalizationProgressNotesByHospitalizationUseCase listByHospitalizationUseCase;
    @MockitoBean
    private DeleteHospitalizationProgressNoteUseCase deleteUseCase;
    @MockitoBean
    private ReactivateHospitalizationProgressNoteUseCase reactivateUseCase;

    private static HospitalizationProgressNoteDto notaEvolucion() {
        return new HospitalizationProgressNoteDto(500L,
                "Paciente estable, buena respuesta al tratamiento",
                new HospitalizationSummaryDto(55L, LocalDate.of(2026, 3, 1)),
                new EmployeeSummaryDto(4L, "EMP-001", "Ana Ruiz"),
                LocalDateTime.of(2026, 3, 1, 9, 15), true);
    }

    @Nested
    @DisplayName("POST /hospitalization-progress-notes")
    class Crear {

        @Test
        @DisplayName("responde 201 con el recurso creado y sus sumarios anidados")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(notaEvolucion());

            mockMvc.perform(post("/hospitalization-progress-notes")
                    .contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.description")
                            .value("Paciente estable, buena respuesta al tratamiento"))
                    .andExpect(jsonPath("$.hospitalization.id").value(55))
                    .andExpect(jsonPath("$.createdBy.employeeCode").value("EMP-001"))
                    .andExpect(jsonPath("$.createdBy.name").value("Ana Ruiz"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando el empleado del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(authz.currentEmployeeId()).thenReturn(EMPLEADO_AUTENTICADO);
            when(createUseCase.execute(any())).thenReturn(notaEvolucion());

            mockMvc.perform(post("/hospitalization-progress-notes")
                    .contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON));

            // El createdById NO viaja en el request: lo pone el backend desde el
            // AuthContext.
            verify(createUseCase).execute(new CreateHospitalizationProgressNoteCommand(
                    "Paciente estable, buena respuesta al tratamiento", 55L, EMPLEADO_AUTENTICADO));
        }

        @Test
        @DisplayName("sin descripcion responde 400 y no llega al caso de uso")
        void sin_descripcion_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-progress-notes")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"description":"   ","hospitalizationId":55}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("con descripcion de mas de 2000 caracteres responde 400")
        void descripcion_demasiado_larga_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-progress-notes")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"description":"%s","hospitalizationId":55}
                            """.formatted("x".repeat(2001)))).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin hospitalizationId responde 400")
        void sin_hospitalization_id_responde_400() throws Exception {
            mockMvc.perform(post("/hospitalization-progress-notes")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"description":"Paciente estable"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una invariante de dominio rota sale como 400, no como 500")
        void invariante_de_dominio_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Hospitalization not found: 55"));

            mockMvc.perform(post("/hospitalization-progress-notes")
                    .contentType(MediaType.APPLICATION_JSON).content(CREAR_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /hospitalization-progress-notes/by-hospitalization/{hospitalizationId}")
    class ListarPorHospitalizacion {

        @Test
        @DisplayName("devuelve la pagina con sus metadatos")
        void devuelve_la_pagina_con_sus_metadatos() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(55L,
                    WebMvcSliceConfig.COMPANY_ID, 1, 5))
                    .thenReturn(new PageResult<>(List.of(notaEvolucion()), 1, 5, 11L, 3));

            mockMvc.perform(get("/hospitalization-progress-notes/by-hospitalization/55")
                    .param("page", "1").param("pageSize", "5")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(500))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("sin parametros usa page=0 y pageSize=20 por defecto")
        void sin_parametros_usa_los_valores_por_defecto() throws Exception {
            when(listByHospitalizationUseCase.listByHospitalization(55L,
                    WebMvcSliceConfig.COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/hospitalization-progress-notes/by-hospitalization/55"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.pageSize").value(20));

            verify(listByHospitalizationUseCase).listByHospitalization(55L,
                    WebMvcSliceConfig.COMPANY_ID, 0, 20);
        }
    }

    @Nested
    @DisplayName("GET /hospitalization-progress-notes/{id}")
    class Buscar {

        @Test
        @DisplayName("acota la busqueda a la company del contexto")
        void acota_la_busqueda_a_la_company_del_contexto() throws Exception {
            when(findUseCase.findById(500L, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(notaEvolucion());

            mockMvc.perform(get("/hospitalization-progress-notes/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(500));

            verify(findUseCase).findById(500L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new HospitalizationProgressNoteNotFoundException(99L));

            mockMvc.perform(get("/hospitalization-progress-notes/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /hospitalization-progress-notes/{id}")
    class Actualizar {

        @Test
        @DisplayName("responde 200 y arma el command con el id de la ruta")
        void responde_200_y_arma_el_command_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(notaEvolucion());

            mockMvc.perform(put("/hospitalization-progress-notes/500")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"description":"Evolucion favorable, se ajusta analgesia"}
                            """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(500));

            // La empresa la pone el controller desde el contexto: nunca viaja en el body.
            verify(updateUseCase).execute(new UpdateHospitalizationProgressNoteCommand(500L,
                    "Evolucion favorable, se ajusta analgesia", WebMvcSliceConfig.COMPANY_ID));
        }

        @Test
        @DisplayName("con descripcion vacia responde 400 y no llega al caso de uso")
        void con_descripcion_vacia_responde_400() throws Exception {
            mockMvc.perform(put("/hospitalization-progress-notes/500")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"description":"   "}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nota inexistente responde 404")
        void nota_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new HospitalizationProgressNoteNotFoundException(500L));

            mockMvc.perform(put("/hospitalization-progress-notes/500")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"description":"Evolucion favorable, se ajusta analgesia"}
                            """)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH")
    class BorrarYReactivar {

        @Test
        @DisplayName("DELETE responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/hospitalization-progress-notes/500"))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(500L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una nota inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new HospitalizationProgressNoteNotFoundException(99L))
                    .when(deleteUseCase).execute(99L, WebMvcSliceConfig.COMPANY_ID);

            mockMvc.perform(delete("/hospitalization-progress-notes/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /enable responde 200 con el recurso reactivado")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(500L, WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(notaEvolucion());

            mockMvc.perform(patch("/hospitalization-progress-notes/500/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(reactivateUseCase).execute(500L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH /enable de una nota inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new HospitalizationProgressNoteNotFoundException(99L));

            mockMvc.perform(patch("/hospitalization-progress-notes/99/enable"))
                    .andExpect(status().isNotFound());
        }
    }
}
