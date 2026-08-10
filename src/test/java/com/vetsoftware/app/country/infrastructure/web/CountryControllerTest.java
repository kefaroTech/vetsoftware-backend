package com.vetsoftware.app.country.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

import com.vetsoftware.app.country.application.command.CreateCountryCommand;
import com.vetsoftware.app.country.application.command.UpdateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.CreateCountryUseCase;
import com.vetsoftware.app.country.application.port.in.DeleteCountryUseCase;
import com.vetsoftware.app.country.application.port.in.FindCountryUseCase;
import com.vetsoftware.app.country.application.port.in.ListCountriesUseCase;
import com.vetsoftware.app.country.application.port.in.ReactivateCountryUseCase;
import com.vetsoftware.app.country.application.port.in.UpdateCountryUseCase;
import com.vetsoftware.app.country.domain.CountryHasActiveChildrenException;
import com.vetsoftware.app.country.domain.CountryNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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

@WebMvcTest(CountryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CountryController — contrato HTTP")
class CountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCountryUseCase createUseCase;
    @MockitoBean
    private UpdateCountryUseCase updateUseCase;
    @MockitoBean
    private FindCountryUseCase findUseCase;
    @MockitoBean
    private ListCountriesUseCase listUseCase;
    @MockitoBean
    private DeleteCountryUseCase deleteUseCase;
    @MockitoBean
    private ReactivateCountryUseCase reactivateUseCase;

    private static CountryDto colombia() {
        return new CountryDto(7L, "Colombia", LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /countries responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(colombia());

        mockMvc.perform(post("/countries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Colombia\"}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Colombia"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("POST /countries traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(colombia());

        mockMvc.perform(post("/countries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Colombia\"}"));

        verify(createUseCase).execute(new CreateCountryCommand("Colombia"));
    }

    @Test
    @DisplayName("POST /countries con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/countries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /countries con nombre de mas de 100 caracteres responde 400")
    void post_con_nombre_demasiado_largo_responde_400() throws Exception {
        mockMvc.perform(post("/countries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "C".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /countries devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(colombia()));

        mockMvc.perform(get("/countries")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Colombia"));
    }

    @Test
    @DisplayName("GET /countries/{id} devuelve el recurso")
    void get_por_id() throws Exception {
        when(findUseCase.findById(7L)).thenReturn(colombia());

        mockMvc.perform(get("/countries/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Colombia"));
    }

    @Test
    @DisplayName("GET /countries/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new CountryNotFoundException(99L));

        mockMvc.perform(get("/countries/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /countries/{id} responde 200 y arma el command con el id de la ruta")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(colombia());

        mockMvc.perform(put("/countries/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Colombia\"}")).andExpect(status().isOk());

        verify(updateUseCase).execute(new UpdateCountryCommand(7L, "Colombia"));
    }

    @Test
    @DisplayName("PUT /countries/{id} con nombre en blanco responde 400")
    void put_con_nombre_en_blanco_responde_400() throws Exception {
        mockMvc.perform(put("/countries/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}")).andExpect(status().isBadRequest());

        verify(updateUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("DELETE /countries/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/countries/7")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(7L);
    }

    @Test
    @DisplayName("DELETE /countries/{id} con departamentos activos responde 409")
    void delete_con_hijos_activos_responde_409() throws Exception {
        doThrow(new CountryHasActiveChildrenException(7L, "state")).when(deleteUseCase).execute(7L);

        mockMvc.perform(delete("/countries/7")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /countries/{id}/enable responde 200 con el pais reactivado")
    void patch_enable_responde_200() throws Exception {
        when(reactivateUseCase.execute(7L)).thenReturn(colombia());

        mockMvc.perform(patch("/countries/7/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
