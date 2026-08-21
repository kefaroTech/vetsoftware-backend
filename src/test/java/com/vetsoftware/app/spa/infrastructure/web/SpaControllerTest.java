package com.vetsoftware.app.spa.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.spa.application.command.CreateSpaCommand;
import com.vetsoftware.app.spa.application.command.UpdateSpaCommand;
import com.vetsoftware.app.spa.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.spa.application.dto.CompanySummaryDto;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.dto.SpaTypeSummaryDto;
import com.vetsoftware.app.spa.application.port.in.CreateSpaUseCase;
import com.vetsoftware.app.spa.application.port.in.DeleteSpaUseCase;
import com.vetsoftware.app.spa.application.port.in.FindSpaUseCase;
import com.vetsoftware.app.spa.application.port.in.ListSpasByAnimalUseCase;
import com.vetsoftware.app.spa.application.port.in.ListSpasUseCase;
import com.vetsoftware.app.spa.application.port.in.UpdateSpaUseCase;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
@WebMvcTest(SpaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SpaController — contrato HTTP")
class SpaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSpaUseCase createUseCase;
    @MockitoBean
    private UpdateSpaUseCase updateUseCase;
    @MockitoBean
    private FindSpaUseCase findUseCase;
    @MockitoBean
    private ListSpasUseCase listUseCase;
    @MockitoBean
    private ListSpasByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private DeleteSpaUseCase deleteUseCase;
    @MockitoBean
    private Authz authz;

    private static SpaDto spaAgendado() {
        return new SpaDto(5L, LocalDate.of(2026, 2, 1), new SpaTypeSummaryDto(20L, "Baño básico"),
                "Baño mensual", "Shampoo hipoalergenico", "Sin novedades", "AGENDADA",
                new AnimalSummaryDto(1L, "Firulais", "A-001"),
                new CompanySummaryDto(10L, "Veterinaria de prueba", "900123456"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /spas responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(createUseCase.execute(any())).thenReturn(spaAgendado());

        mockMvc.perform(post("/spas").contentType(MediaType.APPLICATION_JSON).content("""
                {"date":"2026-02-01","spaTypeId":20,"reason":"Baño mensual",
                 "details":"Shampoo hipoalergenico","observations":"Sin novedades","animalId":1}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("AGENDADA"))
                .andExpect(jsonPath("$.spaType.id").value(20))
                .andExpect(jsonPath("$.animal.id").value(1))
                .andExpect(jsonPath("$.company.id").value(10));
    }

    @Test
    @DisplayName("POST /spas traduce el request al command con el companyId del contexto")
    void post_traduce_el_request_al_command() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(createUseCase.execute(any())).thenReturn(spaAgendado());

        mockMvc.perform(post("/spas").contentType(MediaType.APPLICATION_JSON).content("""
                {"date":"2026-02-01","spaTypeId":20,"reason":"Baño mensual",
                 "details":"Shampoo hipoalergenico","observations":"Sin novedades","animalId":1}
                """));

        verify(createUseCase).execute(new CreateSpaCommand(LocalDate.of(2026, 2, 1), 20L,
                "Baño mensual", "Shampoo hipoalergenico", "Sin novedades", 1L, 10L));
    }

    @Test
    @DisplayName("POST /spas sin date responde 400 y no llega al caso de uso")
    void post_sin_date_responde_400() throws Exception {
        mockMvc.perform(post("/spas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"spaTypeId\":20,\"reason\":\"Baño\",\"details\":\"x\","
                        + "\"observations\":\"x\",\"animalId\":1}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /spas devuelve la lista global")
    void get_lista_global() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(spaAgendado()));

        mockMvc.perform(get("/spas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    @DisplayName("GET /spas/by-animal/{animalId} pagina por empresa del contexto")
    void get_por_animal_pagina_por_empresa() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(listByAnimalUseCase.listByAnimal(eq(1L), eq(10L), eq(null), eq(0), eq(20)))
                .thenReturn(new PageResult<>(List.of(spaAgendado()), 0, 20, 1, 1));

        mockMvc.perform(get("/spas/by-animal/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5));
    }

    @Test
    @DisplayName("GET /spas/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(findUseCase.findById(99L, 10L)).thenThrow(new SpaNotFoundException(99L));

        mockMvc.perform(get("/spas/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /spas/{id} responde 200")
    void put_responde_200() throws Exception {
        when(authz.currentCompanyIdOrNull()).thenReturn(10L);
        when(updateUseCase.execute(any())).thenReturn(spaAgendado());

        mockMvc.perform(put("/spas/5").contentType(MediaType.APPLICATION_JSON).content("""
                {"date":"2026-02-01","spaTypeId":20,"reason":"Baño mensual",
                 "details":"Shampoo hipoalergenico","observations":"Sin novedades","animalId":1}
                """)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /spas/{id} traduce el request al command con el companyId del contexto")
    void put_traduce_el_request_al_command() throws Exception {
        when(authz.currentCompanyIdOrNull()).thenReturn(10L);
        when(updateUseCase.execute(any())).thenReturn(spaAgendado());

        mockMvc.perform(put("/spas/5").contentType(MediaType.APPLICATION_JSON).content("""
                {"date":"2026-02-01","spaTypeId":20,"reason":"Baño mensual",
                 "details":"Shampoo hipoalergenico","observations":"Sin novedades","animalId":1}
                """));

        verify(updateUseCase).execute(new UpdateSpaCommand(5L, LocalDate.of(2026, 2, 1), 20L,
                "Baño mensual", "Shampoo hipoalergenico", "Sin novedades", 1L, 10L));
    }

    @Test
    @DisplayName("DELETE /spas/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        when(authz.currentCompanyIdOrNull()).thenReturn(10L);

        mockMvc.perform(delete("/spas/5")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(5L, 10L);
    }
}
