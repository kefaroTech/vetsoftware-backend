package com.vetsoftware.app.hospitalization.infrastructure.web;

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

import com.vetsoftware.app.hospitalization.application.command.CreateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.command.UpdateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.hospitalization.application.dto.CompanySummaryDto;
import com.vetsoftware.app.hospitalization.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.hospitalization.application.port.in.CreateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.DeleteHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.FindHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsByAnimalUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsByCompanyUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.ReactivateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.UpdateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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
@WebMvcTest(HospitalizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("HospitalizationController — contrato HTTP")
class HospitalizationControllerTest {

    private static final String CREAR_JSON = """
            {"date":"2026-03-01","startDate":"2026-03-01","endDate":"2026-03-05",
             "type":"HOSPITALIZATION","reasonLeaving":"MEDICAL_DISCHARGE",
             "reason":"Gastroenteritis aguda","observations":"Sin complicaciones",
             "animalId":3,"consultationId":7}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateHospitalizationUseCase createUseCase;
    @MockitoBean
    private UpdateHospitalizationUseCase updateUseCase;
    @MockitoBean
    private FindHospitalizationUseCase findUseCase;
    @MockitoBean
    private ListHospitalizationsUseCase listUseCase;
    @MockitoBean
    private ListHospitalizationsByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private ListHospitalizationsByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private DeleteHospitalizationUseCase deleteUseCase;
    @MockitoBean
    private ReactivateHospitalizationUseCase reactivateUseCase;

    private static HospitalizationDto internado() {
        return new HospitalizationDto(55L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 5), HospitalizationType.HOSPITALIZATION,
                ReasonLeaving.MEDICAL_DISCHARGE, "Gastroenteritis aguda", "Sin complicaciones",
                new AnimalSummaryDto(3L, "Firulais", "A-001"),
                new ConsultationSummaryDto(7L, LocalDate.of(2026, 2, 28)),
                new CompanySummaryDto(9L, "Clinica Vet", "900123456"),
                LocalDateTime.of(2026, 3, 1, 9, 15), true);
    }

    private static HospitalizationDto ambulatorio() {
        return new HospitalizationDto(56L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), null,
                HospitalizationType.OUTPATIENT, null, "Control", null,
                new AnimalSummaryDto(3L, "Firulais", "A-001"), null,
                new CompanySummaryDto(9L, "Clinica Vet", "900123456"),
                LocalDateTime.of(2026, 3, 1, 9, 15), true);
    }

    @Nested
    @DisplayName("POST /hospitalizations")
    class Crear {

        @Test
        @DisplayName("responde 201 con el recurso creado y sus sumarios anidados")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(internado());

            mockMvc.perform(post("/hospitalizations").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.type").value("HOSPITALIZATION"))
                    .andExpect(jsonPath("$.reasonLeaving").value("MEDICAL_DISCHARGE"))
                    .andExpect(jsonPath("$.reason").value("Gastroenteritis aguda"))
                    .andExpect(jsonPath("$.startDate").value("2026-03-01"))
                    .andExpect(jsonPath("$.endDate").value("2026-03-05"))
                    .andExpect(jsonPath("$.animal.name").value("Firulais"))
                    .andExpect(jsonPath("$.animal.code").value("A-001"))
                    .andExpect(jsonPath("$.consultation.id").value(7))
                    .andExpect(jsonPath("$.company.identifier").value("900123456"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando la company del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(internado());

            mockMvc.perform(post("/hospitalizations").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON));

            // El companyId NO viaja en el request: lo pone el backend desde el AuthContext.
            verify(createUseCase).execute(new CreateHospitalizationCommand(LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                    HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                    "Gastroenteritis aguda", "Sin complicaciones", 3L, 7L,
                    WebMvcSliceConfig.COMPANY_ID, null, null));
        }

        @Test
        @DisplayName("el peso opcional viaja al command con su unidad")
        void el_peso_opcional_viaja_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(internado());

            mockMvc.perform(
                    post("/hospitalizations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","startDate":"2026-03-01","type":"HOSPITALIZATION",
                             "reason":"Gastroenteritis aguda","animalId":3,
                             "weight":12.5,"weightUnit":"KILOGRAMS"}
                            """)).andExpect(status().isCreated());

            verify(createUseCase).execute(new CreateHospitalizationCommand(LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 1), null, HospitalizationType.HOSPITALIZATION, null,
                    "Gastroenteritis aguda", null, 3L, null, WebMvcSliceConfig.COMPANY_ID,
                    new BigDecimal("12.5"), "KILOGRAMS"));
        }

        @Test
        @DisplayName("sin motivo responde 400 y no llega al caso de uso")
        void sin_motivo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/hospitalizations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","startDate":"2026-03-01","type":"HOSPITALIZATION",
                             "reason":"   ","animalId":3}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("sin animalId responde 400")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/hospitalizations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","startDate":"2026-03-01","type":"HOSPITALIZATION",
                             "reason":"Gastroenteritis aguda"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("con motivo de mas de 500 caracteres responde 400")
        void motivo_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/hospitalizations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","startDate":"2026-03-01","type":"HOSPITALIZATION",
                             "reason":"%s","animalId":3}
                            """.formatted("x".repeat(501)))).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("con peso negativo responde 400")
        void peso_negativo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/hospitalizations").contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-03-01","startDate":"2026-03-01","type":"HOSPITALIZATION",
                             "reason":"Gastroenteritis aguda","animalId":3,"weight":-2}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("una invariante de dominio rota sale como 400, no como 500")
        void invariante_de_dominio_sale_como_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Animal not found: 3"));

            mockMvc.perform(post("/hospitalizations").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /hospitalizations")
    class Listar {

        @Test
        @DisplayName("devuelve la lista completa")
        void devuelve_la_lista_completa() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(internado(), ambulatorio()));

            mockMvc.perform(get("/hospitalizations")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(55))
                    .andExpect(jsonPath("$[1].id").value(56))
                    .andExpect(jsonPath("$[1].consultation").doesNotExist())
                    .andExpect(jsonPath("$[1].type").value("OUTPATIENT"));
        }

        @Test
        @DisplayName("by-company acota el tablero a la company del contexto")
        void by_company_acota_el_tablero_a_la_company_del_contexto() throws Exception {
            when(listByCompanyUseCase.listByCompany(WebMvcSliceConfig.COMPANY_ID))
                    .thenReturn(List.of(internado()));

            mockMvc.perform(get("/hospitalizations/by-company")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(55));

            verify(listByCompanyUseCase).listByCompany(WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("sin resultados devuelve un array vacio")
        void sin_resultados_devuelve_array_vacio() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/hospitalizations")).andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /hospitalizations/by-animal/{animalId}")
    class ListarPorAnimal {

        @Test
        @DisplayName("devuelve la pagina con sus metadatos")
        void devuelve_la_pagina_con_sus_metadatos() throws Exception {
            when(listByAnimalUseCase.listByAnimal(3L, WebMvcSliceConfig.COMPANY_ID, "gastro", 1, 5))
                    .thenReturn(new PageResult<>(List.of(internado()), 1, 5, 11L, 3));

            mockMvc.perform(get("/hospitalizations/by-animal/3").param("q", "gastro")
                    .param("page", "1").param("pageSize", "5")).andExpect(status().isOk())
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

            mockMvc.perform(get("/hospitalizations/by-animal/3")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.pageSize").value(20));

            verify(listByAnimalUseCase).listByAnimal(3L, WebMvcSliceConfig.COMPANY_ID, null, 0, 20);
        }
    }

    @Nested
    @DisplayName("GET /hospitalizations/{id}")
    class Buscar {

        @Test
        @DisplayName("acota la busqueda a la company del contexto")
        void acota_la_busqueda_a_la_company_del_contexto() throws Exception {
            when(findUseCase.findById(55L, WebMvcSliceConfig.COMPANY_ID)).thenReturn(internado());

            mockMvc.perform(get("/hospitalizations/55")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));

            verify(findUseCase).findById(55L, WebMvcSliceConfig.COMPANY_ID);
        }

        @Test
        @DisplayName("inexistente responde 404, no 500")
        void inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, WebMvcSliceConfig.COMPANY_ID))
                    .thenThrow(new HospitalizationNotFoundException(99L));

            mockMvc.perform(get("/hospitalizations/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /hospitalizations/{id}")
    class Actualizar {

        @Test
        @DisplayName("responde 200 y arma el command con el id de la ruta")
        void responde_200_y_arma_el_command_con_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(internado());

            mockMvc.perform(put("/hospitalizations/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));

            verify(updateUseCase).execute(new UpdateHospitalizationCommand(55L,
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5),
                    HospitalizationType.HOSPITALIZATION, ReasonLeaving.MEDICAL_DISCHARGE,
                    "Gastroenteritis aguda", "Sin complicaciones", 3L, 7L,
                    WebMvcSliceConfig.COMPANY_ID));
        }

        @Test
        @DisplayName("con request invalido responde 400 y no llega al caso de uso")
        void request_invalido_responde_400() throws Exception {
            mockMvc.perform(
                    put("/hospitalizations/55").contentType(MediaType.APPLICATION_JSON).content("""
                            {"startDate":"2026-03-01","type":"HOSPITALIZATION",
                             "reason":"Gastroenteritis aguda","animalId":3}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("hospitalizacion inexistente responde 404")
        void hospitalizacion_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new HospitalizationNotFoundException(55L));

            mockMvc.perform(put("/hospitalizations/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CREAR_JSON)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH")
    class BorrarYReactivar {

        @Test
        @DisplayName("DELETE responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/hospitalizations/55")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(55L);
        }

        @Test
        @DisplayName("DELETE de una hospitalizacion inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new HospitalizationNotFoundException(99L))
                    .when(deleteUseCase).execute(99L);

            mockMvc.perform(delete("/hospitalizations/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /enable responde 200 con el recurso reactivado")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(55L)).thenReturn(internado());

            mockMvc.perform(patch("/hospitalizations/55/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH /enable de una hospitalizacion inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L))
                    .thenThrow(new HospitalizationNotFoundException(99L));

            mockMvc.perform(patch("/hospitalizations/99/enable")).andExpect(status().isNotFound());
        }
    }
}
