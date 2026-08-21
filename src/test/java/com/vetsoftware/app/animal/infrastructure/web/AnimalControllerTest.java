package com.vetsoftware.app.animal.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.CreateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.FindAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.ListAnimalsByOwnerUseCase;
import com.vetsoftware.app.animal.application.port.in.ListAnimalsUseCase;
import com.vetsoftware.app.animal.domain.AnimalNotFoundException;
import com.vetsoftware.app.animal.domain.Gender;
import com.vetsoftware.app.animal.domain.ReproductiveState;
import com.vetsoftware.app.animal.domain.WeightType;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
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
 * Rodaja HTTP de {@link AnimalController}: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * Lo que esta rodaja protege y ninguna otra capa cubre: que {@code companyId}
 * NUNCA viaje en el cuerpo del request — lo sella {@code Authz} — y que el
 * mapeo {@code AnimalDto → AnimalResponse} arme los cuatro companion summaries
 * correctamente.
 */
@WebMvcTest(AnimalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AnimalController — contrato HTTP")
class AnimalControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String ALTA_VALIDA = """
            {"name":"Firulais","code":"A-001","specieId":1,"breedId":2,"ownerId":3,
             "gender":"MALE","weightType":"KILOGRAMS","animalType":"NONE",
             "reproductiveState":"STERILIZED","colorId":4,"bod":"2020-05-10",
             "size":30,"deceased":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateAnimalUseCase createUseCase;
    @MockitoBean
    private FindAnimalUseCase findUseCase;
    @MockitoBean
    private ListAnimalsUseCase listUseCase;
    @MockitoBean
    private ListAnimalsByOwnerUseCase listByOwnerUseCase;

    private static AnimalDto perroSano() {
        return AnimalDto.from(AnimalMother.perroSano());
    }

    @Nested
    @DisplayName("POST /animals")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el animal creado y sus companion summaries")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(perroSano());

            mockMvc.perform(
                    post("/animals").contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(AnimalMother.ANIMAL_ID))
                    .andExpect(jsonPath("$.name").value("Firulais"))
                    .andExpect(jsonPath("$.specie.name").value("Perro"))
                    .andExpect(jsonPath("$.breed.name").value("Labrador"))
                    .andExpect(jsonPath("$.owner.document").value("CC-1020"))
                    .andExpect(jsonPath("$.color.name").value("Negro"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"));
        }

        @Test
        @DisplayName("la empresa la pone el backend, nunca el request")
        void la_empresa_la_pone_el_backend() throws Exception {
            when(createUseCase.execute(any())).thenReturn(perroSano());

            mockMvc.perform(
                    post("/animals").contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA));

            verify(createUseCase).execute(new CreateAnimalCommand("Firulais", "A-001", 1L, 2L, 3L,
                    Gender.MALE, WeightType.KILOGRAMS,
                    com.vetsoftware.app.animal.domain.AnimalType.NONE, ReproductiveState.STERILIZED,
                    4L, java.time.LocalDate.of(2020, 5, 10), null, 30, false, null, COMPANY_ID));
        }

        @Test
        @DisplayName("sin nombre responde 400 y no crea nada")
        void sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/animals").contentType(MediaType.APPLICATION_JSON).content("""
                    {"specieId":1,"breedId":2,"ownerId":3,"gender":"MALE",
                     "weightType":"KILOGRAMS","animalType":"NONE",
                     "reproductiveState":"STERILIZED","colorId":4,"deceased":false}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin especie responde 400 y no crea nada")
        void sin_especie_responde_400() throws Exception {
            mockMvc.perform(post("/animals").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Firulais","breedId":2,"ownerId":3,"gender":"MALE",
                     "weightType":"KILOGRAMS","animalType":"NONE",
                     "reproductiveState":"STERILIZED","colorId":4,"deceased":false}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /animals")
    class Listado {

        @Test
        @DisplayName("devuelve la envoltura paginada")
        void devuelve_la_envoltura_paginada() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(perroSano()), 0, 20, 1L, 1));

            mockMvc.perform(get("/animals")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Firulais"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("sin parametros consulta pagina 0, tamano 20")
        void sin_parametros_consulta_pagina_0_tamano_20() throws Exception {
            when(listUseCase.listAll(COMPANY_ID, 0, 20))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/animals")).andExpect(status().isOk());

            verify(listUseCase).listAll(COMPANY_ID, 0, 20);
        }
    }

    @Nested
    @DisplayName("GET /animals/by-owner/{ownerId}")
    class ListadoPorPropietario {

        @Test
        @DisplayName("devuelve los animales del propietario")
        void devuelve_los_animales_del_propietario() throws Exception {
            when(listByOwnerUseCase.listByOwner(AnimalMother.DUENO.id(), COMPANY_ID))
                    .thenReturn(List.of(perroSano()));

            mockMvc.perform(get("/animals/by-owner/" + AnimalMother.DUENO.id()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].owner.id").value(AnimalMother.DUENO.id()));
        }
    }

    @Nested
    @DisplayName("GET /animals/{id}")
    class Busqueda {

        @Test
        @DisplayName("devuelve el detalle del animal")
        void devuelve_el_detalle() throws Exception {
            when(findUseCase.findById(AnimalMother.ANIMAL_ID, COMPANY_ID)).thenReturn(perroSano());

            mockMvc.perform(get("/animals/" + AnimalMother.ANIMAL_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(AnimalMother.ANIMAL_ID));
        }

        @Test
        @DisplayName("un animal inexistente responde 404, no 500")
        void un_animal_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L, COMPANY_ID))
                    .thenThrow(new AnimalNotFoundException(999L));

            mockMvc.perform(get("/animals/999")).andExpect(status().isNotFound());
        }
    }
}
