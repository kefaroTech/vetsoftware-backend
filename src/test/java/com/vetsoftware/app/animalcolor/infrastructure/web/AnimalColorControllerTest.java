package com.vetsoftware.app.animalcolor.infrastructure.web;

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

import com.vetsoftware.app.animalcolor.application.command.CreateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.dto.SpecieSummaryDto;
import com.vetsoftware.app.animalcolor.application.port.in.CreateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.DeleteAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.FindAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.ListAnimalColorsBySpecieUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.ListAnimalColorsUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.ReactivateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.UpdateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.domain.AnimalColorNotFoundException;
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
@WebMvcTest(AnimalColorController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AnimalColorController — contrato HTTP")
class AnimalColorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateAnimalColorUseCase createUseCase;
    @MockitoBean
    private UpdateAnimalColorUseCase updateUseCase;
    @MockitoBean
    private FindAnimalColorUseCase findUseCase;
    @MockitoBean
    private ListAnimalColorsUseCase listUseCase;
    @MockitoBean
    private ListAnimalColorsBySpecieUseCase listBySpecieUseCase;
    @MockitoBean
    private DeleteAnimalColorUseCase deleteUseCase;
    @MockitoBean
    private ReactivateAnimalColorUseCase reactivateUseCase;

    private static AnimalColorDto negro() {
        return new AnimalColorDto(2L, "Negro", new SpecieSummaryDto(1L, "Perro"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /animal-colors responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(negro());

        mockMvc.perform(post("/animal-colors").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Negro\",\"specieId\":1}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.name").value("Negro"))
                .andExpect(jsonPath("$.specie.name").value("Perro"));
    }

    @Test
    @DisplayName("POST /animal-colors traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(negro());

        mockMvc.perform(post("/animal-colors").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Negro\",\"specieId\":1}"));

        verify(createUseCase).execute(new CreateAnimalColorCommand("Negro", 1L));
    }

    @Test
    @DisplayName("POST /animal-colors con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/animal-colors").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"specieId\":1}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /animal-colors devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(negro()));

        mockMvc.perform(get("/animal-colors")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /species/{id}/animal-colors acota por especie")
    void get_lista_por_especie() throws Exception {
        when(listBySpecieUseCase.listBySpecie(1L)).thenReturn(List.of(negro()));

        mockMvc.perform(get("/species/1/animal-colors")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specie.id").value(1));
    }

    @Test
    @DisplayName("GET /animal-colors/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new AnimalColorNotFoundException(99L));

        mockMvc.perform(get("/animal-colors/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /animal-colors/{id} responde 200")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(negro());

        mockMvc.perform(put("/animal-colors/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Negro\",\"specieId\":1}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /animal-colors/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/animal-colors/2")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(2L);
    }

    @Test
    @DisplayName("PATCH /animal-colors/{id}/enable responde 200 con el color reactivado")
    void patch_enable_responde_200_con_el_color_reactivado() throws Exception {
        when(reactivateUseCase.execute(2L)).thenReturn(negro());

        mockMvc.perform(patch("/animal-colors/2/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(2L);
    }
}
