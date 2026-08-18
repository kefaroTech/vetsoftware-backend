package com.vetsoftware.app.state.infrastructure.web;

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

import com.vetsoftware.app.state.application.command.CreateStateCommand;
import com.vetsoftware.app.state.application.command.UpdateStateCommand;
import com.vetsoftware.app.state.application.dto.CountrySummaryDto;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.CreateStateUseCase;
import com.vetsoftware.app.state.application.port.in.DeleteStateUseCase;
import com.vetsoftware.app.state.application.port.in.FindStateUseCase;
import com.vetsoftware.app.state.application.port.in.ListStatesByCountryUseCase;
import com.vetsoftware.app.state.application.port.in.ListStatesUseCase;
import com.vetsoftware.app.state.application.port.in.ReactivateStateUseCase;
import com.vetsoftware.app.state.application.port.in.UpdateStateUseCase;
import com.vetsoftware.app.state.domain.StateHasActiveChildrenException;
import com.vetsoftware.app.state.domain.StateNotFoundException;
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

@WebMvcTest(StateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("StateController — contrato HTTP")
class StateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateStateUseCase createUseCase;
    @MockitoBean
    private UpdateStateUseCase updateUseCase;
    @MockitoBean
    private FindStateUseCase findUseCase;
    @MockitoBean
    private ListStatesUseCase listUseCase;
    @MockitoBean
    private ListStatesByCountryUseCase listByCountryUseCase;
    @MockitoBean
    private DeleteStateUseCase deleteUseCase;
    @MockitoBean
    private ReactivateStateUseCase reactivateUseCase;

    private static StateDto antioquia() {
        return new StateDto(7L, "Antioquia", new CountrySummaryDto(1L, "Colombia"), "05",
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /states responde 201 con el recurso creado, con el pais anidado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(antioquia());

        mockMvc.perform(post("/states").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Antioquia\",\"countryId\":1,\"daneCode\":\"05\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Antioquia"))
                .andExpect(jsonPath("$.country.id").value(1))
                .andExpect(jsonPath("$.country.name").value("Colombia"))
                .andExpect(jsonPath("$.daneCode").value("05"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("POST /states traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(antioquia());

        mockMvc.perform(post("/states").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Antioquia\",\"countryId\":1,\"daneCode\":\"05\"}"));

        verify(createUseCase).execute(new CreateStateCommand("Antioquia", 1L, "05"));
    }

    @Test
    @DisplayName("POST /states con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/states").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"countryId\":1}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /states sin countryId responde 400")
    void post_sin_country_id_responde_400() throws Exception {
        mockMvc.perform(post("/states").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Antioquia\"}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /states devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(antioquia()));

        mockMvc.perform(get("/states")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].name").value("Antioquia"));
    }

    @Test
    @DisplayName("GET /countries/{countryId}/states devuelve solo los del pais pedido")
    void get_por_pais() throws Exception {
        when(listByCountryUseCase.listByCountry(1L)).thenReturn(List.of(antioquia()));

        mockMvc.perform(get("/countries/1/states")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country.id").value(1));
    }

    @Test
    @DisplayName("GET /states/{id} devuelve el recurso")
    void get_por_id() throws Exception {
        when(findUseCase.findById(7L)).thenReturn(antioquia());

        mockMvc.perform(get("/states/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Antioquia"));
    }

    @Test
    @DisplayName("GET /states/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new StateNotFoundException(99L));

        mockMvc.perform(get("/states/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /states/{id} responde 200 y arma el command con el id de la ruta")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(antioquia());

        mockMvc.perform(put("/states/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Antioquia\",\"countryId\":1,\"daneCode\":\"05\"}"))
                .andExpect(status().isOk());

        verify(updateUseCase).execute(new UpdateStateCommand(7L, "Antioquia", 1L, "05"));
    }

    @Test
    @DisplayName("PUT /states/{id} con nombre en blanco responde 400")
    void put_con_nombre_en_blanco_responde_400() throws Exception {
        mockMvc.perform(put("/states/7").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"countryId\":1}")).andExpect(status().isBadRequest());

        verify(updateUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("DELETE /states/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/states/7")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(7L);
    }

    @Test
    @DisplayName("DELETE /states/{id} con municipios activos responde 409")
    void delete_con_hijos_activos_responde_409() throws Exception {
        doThrow(new StateHasActiveChildrenException(7L, "city")).when(deleteUseCase).execute(7L);

        mockMvc.perform(delete("/states/7")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /states/{id}/enable responde 200 con el departamento reactivado")
    void patch_enable_responde_200() throws Exception {
        when(reactivateUseCase.execute(7L)).thenReturn(antioquia());

        mockMvc.perform(patch("/states/7/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
