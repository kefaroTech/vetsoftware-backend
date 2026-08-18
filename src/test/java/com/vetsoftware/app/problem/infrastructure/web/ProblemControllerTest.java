package com.vetsoftware.app.problem.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.problem.application.command.CreateProblemCommand;
import com.vetsoftware.app.problem.application.command.UpdateProblemCommand;
import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.in.CreateProblemUseCase;
import com.vetsoftware.app.problem.application.port.in.DeleteProblemUseCase;
import com.vetsoftware.app.problem.application.port.in.ListProblemsByAnimalUseCase;
import com.vetsoftware.app.problem.application.port.in.UpdateProblemUseCase;
import com.vetsoftware.app.problem.application.query.ListProblemsByAnimalQuery;
import com.vetsoftware.app.problem.domain.ProblemNotFoundException;
import com.vetsoftware.app.problem.domain.ProblemStatus;
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
 * Rodaja HTTP del controller de problemas clinicos: rutas, binding, validacion
 * del request, codigos de estado y forma del JSON. Lo que hay debajo son dobles
 * de los puertos de entrada.
 *
 * <p>
 * La asercion que sostiene el multi-tenant vive aqui: el {@code companyId}
 * <b>no</b> viaja en el cuerpo, lo pone {@code Authz}. Cada verificacion de
 * command comprueba que llega {@link WebMvcSliceConfig#COMPANY_ID}.
 */
@WebMvcTest(ProblemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ProblemController — contrato HTTP")
class ProblemControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final Long PROBLEM_ID = 200L;
    private static final Long ANIMAL_ID = 100L;
    private static final LocalDate INICIO = LocalDate.of(2026, 1, 10);
    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private static final String CUERPO_VALIDO = """
            {"animalId":100,"description":"Dermatitis alergica","status":"ACTIVE",
             "onsetDate":"2026-01-10","notes":"Revisar en dos semanas"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateProblemUseCase createUseCase;
    @MockitoBean
    private UpdateProblemUseCase updateUseCase;
    @MockitoBean
    private DeleteProblemUseCase deleteUseCase;
    @MockitoBean
    private ListProblemsByAnimalUseCase listByAnimalUseCase;

    private static ProblemDto problema(ProblemStatus estado, LocalDate resolvedDate) {
        return new ProblemDto(PROBLEM_ID, ANIMAL_ID, "Firulais", "Dermatitis alergica", estado,
                INICIO, resolvedDate, "Revisar en dos semanas", CREADO, true);
    }

    private static ProblemDto activo() {
        return problema(ProblemStatus.ACTIVE, null);
    }

    @Nested
    @DisplayName("POST /problems")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el problema creado")
        void responde_201_con_el_problema_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(200))
                    .andExpect(jsonPath("$.animalId").value(100))
                    .andExpect(jsonPath("$.animalName").value("Firulais"))
                    .andExpect(jsonPath("$.description").value("Dermatitis alergica"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.onsetDate").value("2026-01-10"))
                    .andExpect(jsonPath("$.resolvedDate").doesNotExist())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la empresa del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            // El cuerpo no lleva companyId: si el controller lo aceptara del cliente,
            // cualquiera podria crear un problema contra otra empresa.
            verify(createUseCase).execute(new CreateProblemCommand(ANIMAL_ID, "Dermatitis alergica",
                    ProblemStatus.ACTIVE, INICIO, null, "Revisar en dos semanas", COMPANY_ID));
        }

        @Test
        @DisplayName("sin animalId responde 400 y no llega al caso de uso")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON).content("""
                    {"description":"Dermatitis","status":"ACTIVE"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin descripcion responde 400")
        void sin_descripcion_responde_400() throws Exception {
            mockMvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON).content("""
                    {"animalId":100,"status":"ACTIVE"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("descripcion de mas de 255 caracteres responde 400")
        void descripcion_muy_larga_responde_400() throws Exception {
            String descripcionLarga = "x".repeat(256);

            mockMvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON).content("""
                    {"animalId":100,"description":"%s","status":"ACTIVE"}
                    """.formatted(descripcionLarga))).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin estado responde 400")
        void sin_estado_responde_400() throws Exception {
            mockMvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON).content("""
                    {"animalId":100,"description":"Dermatitis"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un animal inexistente sale como 400, no 500")
        void animal_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Animal not found: 100"));

            mockMvc.perform(post("/problems").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /problems/by-animal/{animalId}")
    class ListadoPorAnimal {

        @Test
        @DisplayName("devuelve la envoltura paginada de la empresa del contexto")
        void devuelve_la_envoltura_paginada() throws Exception {
            when(listByAnimalUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(activo()), 0, 20, 1L, 1));

            mockMvc.perform(get("/problems/by-animal/100")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(200))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("sin parametros usa pagina 0 y tamano 20")
        void sin_parametros_usa_pagina_0_y_tamano_20() throws Exception {
            when(listByAnimalUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/problems/by-animal/100")).andExpect(status().isOk());

            verify(listByAnimalUseCase)
                    .execute(new ListProblemsByAnimalQuery(ANIMAL_ID, COMPANY_ID, 0, 20));
        }

        @Test
        @DisplayName("traslada la pagina y el tamano pedidos")
        void traslada_la_pagina_y_el_tamano_pedidos() throws Exception {
            when(listByAnimalUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 1, 5, 0L, 0));

            mockMvc.perform(
                    get("/problems/by-animal/100").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk());

            verify(listByAnimalUseCase)
                    .execute(new ListProblemsByAnimalQuery(ANIMAL_ID, COMPANY_ID, 1, 5));
        }
    }

    @Nested
    @DisplayName("PUT /problems/{id}")
    class Actualizacion {

        private static final String CUERPO_VALIDO_UPDATE = """
                {"description":"Resuelto tras tratamiento","status":"RESOLVED",
                 "onsetDate":"2026-01-10","resolvedDate":"2026-02-01"}
                """;

        @Test
        @DisplayName("responde 200 con el problema actualizado")
        void responde_200_con_el_problema_actualizado() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenReturn(problema(ProblemStatus.RESOLVED, LocalDate.of(2026, 2, 1)));

            mockMvc.perform(put("/problems/200").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(200))
                    .andExpect(jsonPath("$.status").value("RESOLVED"))
                    .andExpect(jsonPath("$.resolvedDate").value("2026-02-01"));
        }

        @Test
        @DisplayName("traduce el request al command con el id de la ruta y la empresa del contexto")
        void traduce_el_request_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenReturn(problema(ProblemStatus.RESOLVED, LocalDate.of(2026, 2, 1)));

            mockMvc.perform(put("/problems/200").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE));

            verify(updateUseCase).execute(new UpdateProblemCommand(PROBLEM_ID,
                    "Resuelto tras tratamiento", ProblemStatus.RESOLVED, INICIO,
                    LocalDate.of(2026, 2, 1), null, COMPANY_ID));
        }

        @Test
        @DisplayName("sin descripcion responde 400")
        void sin_descripcion_responde_400() throws Exception {
            mockMvc.perform(put("/problems/200").contentType(MediaType.APPLICATION_JSON).content("""
                    {"status":"ACTIVE"}
                    """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un problema inexistente responde 404")
        void problema_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new ProblemNotFoundException(200L));

            mockMvc.perform(put("/problems/200").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /problems/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204_sin_cuerpo() throws Exception {
            mockMvc.perform(delete("/problems/200")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(PROBLEM_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("un problema inexistente responde 404")
        void problema_inexistente_responde_404() throws Exception {
            doThrow(new ProblemNotFoundException(200L)).when(deleteUseCase).execute(PROBLEM_ID,
                    COMPANY_ID);

            mockMvc.perform(delete("/problems/200")).andExpect(status().isNotFound());
        }
    }
}
