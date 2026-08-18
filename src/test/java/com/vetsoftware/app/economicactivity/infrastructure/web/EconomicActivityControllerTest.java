package com.vetsoftware.app.economicactivity.infrastructure.web;

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

import com.vetsoftware.app.economicactivity.application.command.CreateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.command.UpdateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.in.CreateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.DeleteEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.FindEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.ListEconomicActivitiesUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.ReactivateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.UpdateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.domain.EconomicActivityNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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
 * Rodaja HTTP del controller de actividades economicas: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 */
@WebMvcTest(EconomicActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("EconomicActivityController — contrato HTTP")
class EconomicActivityControllerTest {

    private static final String CUERPO_VALIDO = """
            {"code":"0111","name":"Cultivo de cereales"}
            """;

    private static final String NOMBRE_DE_151_CARACTERES = "x".repeat(151);
    private static final String CODIGO_DE_21_CARACTERES = "x".repeat(21);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateEconomicActivityUseCase createUseCase;
    @MockitoBean
    private UpdateEconomicActivityUseCase updateUseCase;
    @MockitoBean
    private FindEconomicActivityUseCase findUseCase;
    @MockitoBean
    private ListEconomicActivitiesUseCase listUseCase;
    @MockitoBean
    private DeleteEconomicActivityUseCase deleteUseCase;
    @MockitoBean
    private ReactivateEconomicActivityUseCase reactivateUseCase;

    private static EconomicActivityDto cultivoDeCereales() {
        return new EconomicActivityDto(70L, "0111", "Cultivo de cereales",
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Nested
    @DisplayName("POST /economic-activities")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cultivoDeCereales());

            mockMvc.perform(post("/economic-activities").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.code").value("0111"))
                    .andExpect(jsonPath("$.name").value("Cultivo de cereales"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(cultivoDeCereales());

            mockMvc.perform(post("/economic-activities").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(createUseCase)
                    .execute(new CreateEconomicActivityCommand("0111", "Cultivo de cereales"));
        }

        @Test
        @DisplayName("codigo vacio responde 400 y no llega al caso de uso")
        void codigo_vacio_responde_400() throws Exception {
            mockMvc.perform(
                    post("/economic-activities").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"","name":"Cultivo de cereales"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("codigo de mas de 20 caracteres responde 400")
        void codigo_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/economic-activities").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + CODIGO_DE_21_CARACTERES
                            + "\",\"name\":\"Cultivo de cereales\"}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(
                    post("/economic-activities").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"0111","name":""}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre de mas de 150 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/economic-activities").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"0111\",\"name\":\"" + NOMBRE_DE_151_CARACTERES + "\"}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un codigo repetido sale como 400, no como 500")
        void codigo_repetido_responde_400() throws Exception {
            when(createUseCase.execute(any())).thenThrow(
                    new IllegalArgumentException("EconomicActivity code already exists: 0111"));

            mockMvc.perform(post("/economic-activities").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /economic-activities lista todas las actividades")
        void get_lista_todas_las_actividades() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(cultivoDeCereales()));

            mockMvc.perform(get("/economic-activities")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(70))
                    .andExpect(jsonPath("$[0].code").value("0111"));
        }

        @Test
        @DisplayName("GET /economic-activities/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(70L)).thenReturn(cultivoDeCereales());

            mockMvc.perform(get("/economic-activities/70")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.name").value("Cultivo de cereales"));
        }

        @Test
        @DisplayName("GET /economic-activities/{id} inexistente responde 404")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L)).thenThrow(new EconomicActivityNotFoundException(99L));

            mockMvc.perform(get("/economic-activities/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre una actividad existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /economic-activities/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cultivoDeCereales());

            mockMvc.perform(put("/economic-activities/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(cultivoDeCereales());

            mockMvc.perform(put("/economic-activities/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(updateUseCase)
                    .execute(new UpdateEconomicActivityCommand(70L, "0111", "Cultivo de cereales"));
        }

        @Test
        @DisplayName("PUT con nombre vacio responde 400 y no llega al caso de uso")
        void put_con_nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(put("/economic-activities/70").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"code":"0111","name":""}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("PUT sobre una actividad inexistente responde 404")
        void put_sobre_actividad_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new EconomicActivityNotFoundException(70L));

            mockMvc.perform(put("/economic-activities/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /economic-activities/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/economic-activities/70")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(70L);
        }

        @Test
        @DisplayName("DELETE de una actividad inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new EconomicActivityNotFoundException(99L))
                    .when(deleteUseCase).execute(99L);

            mockMvc.perform(delete("/economic-activities/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PATCH /economic-activities/{id}/enable reactiva y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(70L)).thenReturn(cultivoDeCereales());

            mockMvc.perform(patch("/economic-activities/70/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de una actividad inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L))
                    .thenThrow(new EconomicActivityNotFoundException(99L));

            mockMvc.perform(patch("/economic-activities/99/enable"))
                    .andExpect(status().isNotFound());
        }
    }
}
