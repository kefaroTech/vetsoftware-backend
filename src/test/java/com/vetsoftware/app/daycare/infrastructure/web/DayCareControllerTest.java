package com.vetsoftware.app.daycare.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.daycare.application.dto.CompanySummaryDto;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.CreateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.DeleteDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.FindDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresByAnimalUseCase;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresUseCase;
import com.vetsoftware.app.daycare.application.port.in.ReactivateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.UpdateDayCareUseCase;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import com.vetsoftware.app.daycare.domain.DayCareType;
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
@WebMvcTest(DayCareController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DayCareController — contrato HTTP")
class DayCareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDayCareUseCase createUseCase;
    @MockitoBean
    private UpdateDayCareUseCase updateUseCase;
    @MockitoBean
    private FindDayCareUseCase findUseCase;
    @MockitoBean
    private ListDayCaresUseCase listUseCase;
    @MockitoBean
    private ListDayCaresByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private DeleteDayCareUseCase deleteUseCase;
    @MockitoBean
    private ReactivateDayCareUseCase reactivateUseCase;
    @MockitoBean
    private Authz authz;

    private static DayCareDto guarderia() {
        return new DayCareDto(5L, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3), DayCareType.DAYCARE, "Correa", "Sin novedades",
                new AnimalSummaryDto(1L, "Firulais", "A-001"),
                new CompanySummaryDto(10L, "Veterinaria de prueba", "900123456"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /daycares responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(createUseCase.execute(any())).thenReturn(guarderia());

        mockMvc.perform(post("/daycares").contentType(MediaType.APPLICATION_JSON).content("""
                {"date":"2026-02-01","startDate":"2026-02-01","endDate":"2026-02-03",
                 "type":"DAYCARE","objects":"Correa","observations":"Sin novedades","animalId":1}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.type").value("DAYCARE"))
                .andExpect(jsonPath("$.animal.id").value(1))
                .andExpect(jsonPath("$.company.id").value(10));
    }

    @Test
    @DisplayName("POST /daycares traduce el request al command con el companyId del contexto")
    void post_traduce_el_request_al_command() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(createUseCase.execute(any())).thenReturn(guarderia());

        mockMvc.perform(post("/daycares").contentType(MediaType.APPLICATION_JSON).content("""
                {"date":"2026-02-01","startDate":"2026-02-01","endDate":"2026-02-03",
                 "type":"DAYCARE","objects":"Correa","observations":"Sin novedades","animalId":1}
                """));

        verify(createUseCase).execute(new CreateDayCareCommand(LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 3), DayCareType.DAYCARE, "Correa",
                "Sin novedades", 1L, 10L));
    }

    @Test
    @DisplayName("POST /daycares sin date responde 400 y no llega al caso de uso")
    void post_sin_date_responde_400() throws Exception {
        mockMvc.perform(post("/daycares").contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"2026-02-01\",\"type\":\"DAYCARE\",\"animalId\":1}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /daycares devuelve la lista global")
    void get_lista_global() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(guarderia()));

        mockMvc.perform(get("/daycares")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));
    }

    @Test
    @DisplayName("GET /daycares/by-animal/{animalId} pagina por empresa del contexto")
    void get_por_animal_pagina_por_empresa() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(listByAnimalUseCase.listByAnimal(eq(1L), eq(10L), eq(null), eq(0), eq(20)))
                .thenReturn(new PageResult<>(List.of(guarderia()), 0, 20, 1, 1));

        mockMvc.perform(get("/daycares/by-animal/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5));
    }

    @Test
    @DisplayName("GET /daycares/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(findUseCase.findById(99L, 10L)).thenThrow(new DayCareNotFoundException(99L));

        mockMvc.perform(get("/daycares/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /daycares/{id} responde 200 con el recurso encontrado")
    void get_por_id_responde_200() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(findUseCase.findById(5L, 10L)).thenReturn(guarderia());

        mockMvc.perform(get("/daycares/5")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @DisplayName("PUT /daycares/{id} responde 200")
    void put_responde_200() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(updateUseCase.execute(any())).thenReturn(guarderia());

        mockMvc.perform(put("/daycares/5").contentType(MediaType.APPLICATION_JSON).content("""
                {"date":"2026-02-01","startDate":"2026-02-01","endDate":"2026-02-03",
                 "type":"DAYCARE","objects":"Correa","observations":"Sin novedades","animalId":1}
                """)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /daycares/{id} responde 204 y delega con la empresa del contexto")
    void delete_responde_204() throws Exception {
        when(authz.currentCompanyIdOrNull()).thenReturn(10L);

        mockMvc.perform(delete("/daycares/5")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(5L, 10L);
    }

    @Test
    @DisplayName("PATCH /daycares/{id}/enable responde 200 con el daycare reactivado")
    void patch_enable_responde_200() throws Exception {
        when(authz.currentCompanyId()).thenReturn(10L);
        when(reactivateUseCase.execute(5L, 10L)).thenReturn(guarderia());

        mockMvc.perform(patch("/daycares/5/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(5L, 10L);
    }
}
