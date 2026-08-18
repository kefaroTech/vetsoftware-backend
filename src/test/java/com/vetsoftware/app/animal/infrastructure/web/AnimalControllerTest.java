package com.vetsoftware.app.animal.infrastructure.web;

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

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.port.in.CreateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.DeleteAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.FindAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.ListAnimalsByOwnerUseCase;
import com.vetsoftware.app.animal.application.port.in.ListAnimalsUseCase;
import com.vetsoftware.app.animal.application.port.in.ReactivateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.UpdateAnimalUseCase;
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
    private UpdateAnimalUseCase updateUseCase;
    @MockitoBean
    private FindAnimalUseCase findUseCase;
    @MockitoBean
    private ListAnimalsUseCase listUseCase;
    @MockitoBean
    private ListAnimalsByOwnerUseCase listByOwnerUseCase;
    @MockitoBean
    private DeleteAnimalUseCase deleteUseCase;
    @MockitoBean
    private ReactivateAnimalUseCase reactivateUseCase;

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

    @Nested
    @DisplayName("PUT /animals/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 con el animal actualizado")
        void responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(perroSano());

            mockMvc.perform(put("/animals/" + AnimalMother.ANIMAL_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Firulais"));
        }

        @Test
        @DisplayName("el id sale de la ruta y la empresa del contexto, nunca del cuerpo")
        void el_id_sale_de_la_ruta_y_la_empresa_del_contexto() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(perroSano());

            mockMvc.perform(put("/animals/" + AnimalMother.ANIMAL_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(ALTA_VALIDA));

            verify(updateUseCase).execute(new UpdateAnimalCommand(AnimalMother.ANIMAL_ID,
                    "Firulais", "A-001", 1L, 2L, 3L, Gender.MALE, WeightType.KILOGRAMS,
                    com.vetsoftware.app.animal.domain.AnimalType.NONE, ReproductiveState.STERILIZED,
                    4L, java.time.LocalDate.of(2020, 5, 10), null, 30, false, null, COMPANY_ID));
        }

        @Test
        @DisplayName("sin nombre responde 400 y no actualiza nada")
        void sin_nombre_responde_400() throws Exception {
            mockMvc.perform(put("/animals/" + AnimalMother.ANIMAL_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"specieId":1,"breedId":2,"ownerId":3,"gender":"MALE",
                             "weightType":"KILOGRAMS","animalType":"NONE",
                             "reproductiveState":"STERILIZED","colorId":4,"deceased":false}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("actualizar un animal inexistente responde 404")
        void actualizar_un_animal_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new AnimalNotFoundException(999L));

            mockMvc.perform(put("/animals/999").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /animals/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204() throws Exception {
            mockMvc.perform(delete("/animals/" + AnimalMother.ANIMAL_ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(AnimalMother.ANIMAL_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("borrar un animal con hijos activos responde 409, no 500")
        void borrar_con_hijos_activos_responde_409() throws Exception {
            org.mockito.Mockito
                    .doThrow(new com.vetsoftware.app.animal.domain.AnimalHasActiveChildrenException(
                            AnimalMother.ANIMAL_ID, "consultation"))
                    .when(deleteUseCase).execute(AnimalMother.ANIMAL_ID, COMPANY_ID);

            mockMvc.perform(delete("/animals/" + AnimalMother.ANIMAL_ID))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("borrar un animal inexistente responde 404")
        void borrar_un_animal_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new AnimalNotFoundException(999L)).when(deleteUseCase)
                    .execute(999L, COMPANY_ID);

            mockMvc.perform(delete("/animals/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /animals/{id}/enable")
    class Reactivacion {

        @Test
        @DisplayName("responde 200 con el animal reactivado")
        void responde_200() throws Exception {
            when(reactivateUseCase.execute(AnimalMother.ANIMAL_ID, COMPANY_ID))
                    .thenReturn(perroSano());

            mockMvc.perform(patch("/animals/" + AnimalMother.ANIMAL_ID + "/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("reactivar un animal inexistente responde 404")
        void reactivar_un_animal_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(999L, COMPANY_ID))
                    .thenThrow(new AnimalNotFoundException(999L));

            mockMvc.perform(patch("/animals/999/enable")).andExpect(status().isNotFound());
        }
    }
}
