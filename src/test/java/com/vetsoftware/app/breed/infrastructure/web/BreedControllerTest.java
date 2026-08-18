package com.vetsoftware.app.breed.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.breed.application.command.CreateBreedCommand;
import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.dto.SpecieSummaryDto;
import com.vetsoftware.app.breed.application.port.in.CreateBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.DeleteBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.FindBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.ListBreedsBySpecieUseCase;
import com.vetsoftware.app.breed.application.port.in.ListBreedsUseCase;
import com.vetsoftware.app.breed.application.port.in.ReactivateBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.UpdateBreedUseCase;
import com.vetsoftware.app.breed.domain.BreedNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. Lo que hay debajo son dobles — aqui no se prueba
 * el caso de uso, se prueba el contrato que ve el front.
 */
@WebMvcTest(BreedController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BreedController — contrato HTTP")
class BreedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBreedUseCase createUseCase;
    @MockitoBean
    private UpdateBreedUseCase updateUseCase;
    @MockitoBean
    private FindBreedUseCase findUseCase;
    @MockitoBean
    private ListBreedsUseCase listUseCase;
    @MockitoBean
    private ListBreedsBySpecieUseCase listBySpecieUseCase;
    @MockitoBean
    private DeleteBreedUseCase deleteUseCase;
    @MockitoBean
    private ReactivateBreedUseCase reactivateUseCase;

    private static BreedDto labrador() {
        return new BreedDto(2L, "Labrador", new SpecieSummaryDto(1L, "Perro"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /breeds responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(labrador());

        mockMvc.perform(post("/breeds").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Labrador\",\"specieId\":1}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Labrador"))
                .andExpect(jsonPath("$.specie.name").value("Perro"));
    }

    @Test
    @DisplayName("POST /breeds traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(labrador());

        mockMvc.perform(post("/breeds").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Labrador\",\"specieId\":1}"));

        verify(createUseCase).execute(new CreateBreedCommand("Labrador", 1L));
    }

    @Test
    @DisplayName("POST /breeds con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/breeds").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"specieId\":1}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /breeds devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(labrador()));

        mockMvc.perform(get("/breeds")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /species/{id}/breeds acota por especie")
    void get_lista_por_especie() throws Exception {
        when(listBySpecieUseCase.listBySpecie(1L)).thenReturn(List.of(labrador()));

        mockMvc.perform(get("/species/1/breeds")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specie.id").value(1));
    }

    @Test
    @DisplayName("GET /breeds/{id} existente responde 200 con el recurso")
    void get_por_id_responde_200() throws Exception {
        when(findUseCase.findById(2L)).thenReturn(labrador());

        mockMvc.perform(get("/breeds/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Labrador"));
    }

    @Test
    @DisplayName("GET /breeds/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new BreedNotFoundException(99L));

        mockMvc.perform(get("/breeds/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /breeds/{id} responde 200")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(labrador());

        mockMvc.perform(put("/breeds/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Labrador\",\"specieId\":1}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /breeds/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/breeds/2")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(2L);
    }

    @Test
    @DisplayName("PATCH /breeds/{id}/enable responde 200 con la raza reactivada")
    void patch_enable_responde_200_con_la_raza_reactivada() throws Exception {
        when(reactivateUseCase.execute(2L)).thenReturn(labrador());

        mockMvc.perform(patch("/breeds/2/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(2L);
    }
}
