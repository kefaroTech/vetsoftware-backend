package com.vetsoftware.app.vaccination.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import com.vetsoftware.app.vaccination.application.command.CreateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.command.UpdateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.vaccination.application.dto.CompanySummaryDto;
import com.vetsoftware.app.vaccination.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.dto.VaccinationTypeSummaryDto;
import com.vetsoftware.app.vaccination.application.port.in.CreateVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.in.DeleteVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.in.FindVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.in.ListVaccinationsByAnimalUseCase;
import com.vetsoftware.app.vaccination.application.port.in.ListVaccinationsUseCase;
import com.vetsoftware.app.vaccination.application.port.in.UpdateVaccinationUseCase;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
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
@WebMvcTest(VaccinationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("VaccinationController — contrato HTTP")
class VaccinationControllerTest {

    private static final String CREAR_JSON = """
            {"date":"2026-03-01","vaccinationTypeId":1,"lot":"L-2026-A","notes":"Sin reaccion",
             "route":"Subcutanea","applicationSite":"Cuello","nextVaccination":"2027-03-01",
             "animalId":3,"consultationId":7}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateVaccinationUseCase createUseCase;
    @MockitoBean
    private UpdateVaccinationUseCase updateUseCase;
    @MockitoBean
    private FindVaccinationUseCase findUseCase;
    @MockitoBean
    private ListVaccinationsUseCase listUseCase;
    @MockitoBean
    private ListVaccinationsByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private DeleteVaccinationUseCase deleteUseCase;

    private static VaccinationDto vacunada() {
        return new VaccinationDto(55L, LocalDate.of(2026, 3, 1),
                new VaccinationTypeSummaryDto(1L, "Rabia"), "L-2026-A", "Sin reaccion",
                "Subcutanea", "Cuello", LocalDate.of(2027, 3, 1),
                new AnimalSummaryDto(3L, "Firulais", "A-001"),
                new ConsultationSummaryDto(7L, LocalDate.of(2026, 2, 28)),
                new CompanySummaryDto(9L, "Clinica Vet", "900123456"),
                LocalDateTime.of(2026, 3, 1, 9, 15), true);
    }

    private static VaccinationDto sinConsulta() {
        return new VaccinationDto(56L, LocalDate.of(2026, 3, 1),
                new VaccinationTypeSummaryDto(1L, "Rabia"), "L-2026-C", "Sin reaccion", null, null,
                null, new AnimalSummaryDto(3L, "Firulais", "A-001"), null,
                new CompanySummaryDto(9L, "Clinica Vet", "900123456"),
                LocalDateTime.of(2026, 3, 1, 9, 15), true);
    }

    @Nested
    @DisplayName("POST /vaccinations")
    class Crear {

        @Test
        @DisplayName("responde 201 con el recurso creado y sus sumarios anidados")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(vacunada());

            mockMvc.perform(post("/vaccinations").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.lot").value("L-2026-A"))
                    .andExpect(jsonPath("$.vaccinationType.name").value("Rabia"))
                    .andExpect(jsonPath("$.animal.code").value("A-001"))
                    .andExpect(jsonPath("$.consultation.id").value(7))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la company del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(vacunada());

            mockMvc.perform(post("/vaccinations").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON));

            // El companyId NO viaja en el request: lo pone el backend desde el AuthContext.
            verify(createUseCase).execute(new CreateVaccinationCommand(LocalDate.of(2026, 3, 1), 1L,
                    "L-2026-A", "Sin reaccion", "Subcutanea", "Cuello", LocalDate.of(2027, 3, 1),
                    3L, 7L, WebMvcSliceConfig.COMPANY_ID));
        }

        @Test
        @DisplayName("sin consultationId el campo llega null al command y a la respuesta")
        void sin_consultation_id_llega_null() throws Exception {
            when(createUseCase.execute(any())).thenReturn(sinConsulta());

            mockMvc.perform(post("/vaccinations").contentType(MediaType.APPLICATION_JSON).content(
                    """
                            {"date":"2026-03-01","vaccinationTypeId":1,"lot":"L-2026-C","animalId":3}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.consultation").doesNotExist());

            verify(createUseCase).execute(new CreateVaccinationCommand(LocalDate.of(2026, 3, 1), 1L,
                    "L-2026-C", null, null, null, null, 3L, null, WebMvcSliceConfig.COMPANY_ID));
        }

        @Test
        @DisplayName("sin lote responde 400 y no llega al caso de uso")
        void sin_lote_responde_400() throws Exception {
            mockMvc.perform(
                    post("/vaccinations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","vaccinationTypeId":1,"lot":"   ","animalId":3}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin tipo de vacuna responde 400")
        void sin_tipo_de_vacuna_responde_400() throws Exception {
            mockMvc.perform(
                    post("/vaccinations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","lot":"L-2026-A","animalId":3}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin animalId responde 400")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/vaccinations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","vaccinationTypeId":1,"lot":"L-2026-A"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("con lote de mas de 100 caracteres responde 400")
        void lote_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/vaccinations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","vaccinationTypeId":1,"lot":"%s","animalId":3}
                            """.formatted("x".repeat(101)))).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una invariante de dominio rota sale como 400, no como 500")
        void invariante_de_dominio_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Animal not found: 3"));

            mockMvc.perform(post("/vaccinations").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /vaccinations")
    class Listar {

        @Test
        @DisplayName("devuelve la lista completa")
        void devuelve_la_lista_completa() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(vacunada(), sinConsulta()));

            mockMvc.perform(get("/vaccinations")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(55))
                    .andExpect(jsonPath("$[1].id").value(56))
                    .andExpect(jsonPath("$[1].consultation").doesNotExist());
        }

        @Test
        @DisplayName("sin resultados devuelve un array vacio")
        void sin_resultados_devuelve_array_vacio() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/vaccinations")).andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /vaccinations/by-animal/{animalId}")
    class ListarPorAnimal {

        @Test
        @DisplayName("devuelve la pagina con sus metadatos")
        void devuelve_la_pagina_con_sus_metadatos() throws Exception {
            when(listByAnimalUseCase.listByAnimal(3L, WebMvcSliceConfig.COMPANY_ID, "rabia", 1, 5))
                    .thenReturn(new PageResult<>(List.of(vacunada()), 1, 5, 11L, 3));

            mockMvc.perform(get("/vaccinations/by-animal/3").param("q", "rabia").param("page", "1")
                    .param("pageSize", "5")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(55))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("sin parametros usa page=0 y pageSize=20 por defecto")
        void sin_parametros_usa_los_valores_por_defecto() throws Exception {
            when(listByAnimalUseCase.listByAnimal(3L, WebMvcSliceConfig.COMPANY_ID, null, 0, 20))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/vaccinations/by-animal/3")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageSize").value(20));

            verify(listByAnimalUseCase).listByAnimal(3L, WebMvcSliceConfig.COMPANY_ID, null, 0, 20);
        }
    }

    @Nested
    @DisplayName("GET /vaccinations/{id}")
    class Buscar {

        @Test
        @DisplayName("acota la busqueda a la company del contexto")
        void acota_la_busqueda_a_la_company_del_contexto() throws Exception {
            when(findUseCase.findById(55L, WebMvcSliceConfig.COMPANY_ID)).thenReturn(vacunada());

            mockMvc.perform(get("/vaccinations/55")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));

            verify(findUseCase).findById(55L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new VaccinationNotFoundException(99L));

            mockMvc.perform(get("/vaccinations/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /vaccinations/{id}")
    class Actualizar {

        @Test
        @DisplayName("responde 200 y arma el command con el id de la ruta")
        void responde_200_y_arma_el_command_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(vacunada());

            mockMvc.perform(put("/vaccinations/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));

            verify(updateUseCase).execute(new UpdateVaccinationCommand(55L,
                    LocalDate.of(2026, 3, 1), 1L, "L-2026-A", "Sin reaccion", "Subcutanea",
                    "Cuello", LocalDate.of(2027, 3, 1), 3L, 7L, WebMvcSliceConfig.COMPANY_ID));
        }

        @Test
        @DisplayName("con request invalido responde 400 y no llega al caso de uso")
        void request_invalido_responde_400() throws Exception {
            mockMvc.perform(
                    put("/vaccinations/55").contentType(MediaType.APPLICATION_JSON).content("""
                            {"lot":"L-2026-A","animalId":3}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("vacunacion inexistente responde 404")
        void vacunacion_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new VaccinationNotFoundException(55L));

            mockMvc.perform(put("/vaccinations/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH")
    class BorrarYReactivar {

        @Test
        @DisplayName("DELETE responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/vaccinations/55")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(55L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una vacunacion inexistente en la empresa responde 404")
        void delete_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new VaccinationNotFoundException(99L)).when(deleteUseCase)
                    .execute(99L, WebMvcSliceConfig.COMPANY_ID);

            mockMvc.perform(delete("/vaccinations/99")).andExpect(status().isNotFound());
        }
    }
}
