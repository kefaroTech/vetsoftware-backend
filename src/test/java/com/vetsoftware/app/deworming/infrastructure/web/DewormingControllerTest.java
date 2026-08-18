package com.vetsoftware.app.deworming.infrastructure.web;

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

import com.vetsoftware.app.deworming.application.command.CreateDewormingCommand;
import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.CreateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.DeleteDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.FindDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.ListDewormingsByAnimalUseCase;
import com.vetsoftware.app.deworming.application.port.in.ListDewormingsUseCase;
import com.vetsoftware.app.deworming.application.port.in.ReactivateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.UpdateDewormingUseCase;
import com.vetsoftware.app.deworming.domain.DewormingNotFoundException;
import com.vetsoftware.app.deworming.domain.DewormingType;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
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
 * de estado y forma del JSON. Lo que hay debajo son dobles — aqui no se prueba
 * el caso de uso, se prueba el contrato que ve el front.
 */
@WebMvcTest(DewormingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DewormingController — contrato HTTP")
class DewormingControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;

    private static final String CUERPO_CREACION_VALIDO = """
            {"date":"2026-04-01","lastDeworming":"2026-01-01","type":"INTERNAL",
             "product":"Drontal Plus","dosage":"1 tableta / 10kg","nextControl":"2026-07-01",
             "observations":"Sin reacciones adversas","animalId":100,"consultationId":200}
            """;

    private static final String CUERPO_ACTUALIZACION_VALIDO = """
            {"date":"2026-05-01","lastDeworming":"2026-02-01","type":"EXTERNAL",
             "product":"Bravecto","dosage":"1 comprimido","nextControl":"2026-08-01",
             "observations":"Control en un mes","animalId":101,"consultationId":200}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDewormingUseCase createUseCase;
    @MockitoBean
    private UpdateDewormingUseCase updateUseCase;
    @MockitoBean
    private FindDewormingUseCase findUseCase;
    @MockitoBean
    private ListDewormingsUseCase listUseCase;
    @MockitoBean
    private ListDewormingsByAnimalUseCase listByAnimalUseCase;
    @MockitoBean
    private DeleteDewormingUseCase deleteUseCase;
    @MockitoBean
    private ReactivateDewormingUseCase reactivateUseCase;

    private static DewormingDto dto() {
        return DewormingDto.from(DewormingMother.desparasitacionValida());
    }

    @Nested
    @DisplayName("POST /dewormings")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la desparasitacion creada")
        void responde_201_con_la_desparasitacion_creada() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/dewormings").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREACION_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(DewormingMother.DEWORMING_ID))
                    .andExpect(jsonPath("$.product").value("Drontal Plus"))
                    .andExpect(jsonPath("$.animal.name").value("Firulais"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"));
        }

        @Test
        @DisplayName("traduce el request al command con el companyId del contexto, no del body")
        void traduce_el_request_al_command_con_el_company_id_del_contexto() throws Exception {
            when(createUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(post("/dewormings").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_CREACION_VALIDO));

            verify(createUseCase).execute(new CreateDewormingCommand(LocalDate.of(2026, 4, 1),
                    LocalDate.of(2026, 1, 1), DewormingType.INTERNAL, "Drontal Plus",
                    "1 tableta / 10kg", LocalDate.of(2026, 7, 1), "Sin reacciones adversas", 100L,
                    200L, COMPANY_ID));
        }

        @Test
        @DisplayName("con producto vacio responde 400 y no llega al caso de uso")
        void con_producto_vacio_responde_400() throws Exception {
            mockMvc.perform(post("/dewormings").contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"2026-04-01","type":"INTERNAL","product":"",
                     "dosage":"1 tableta","animalId":100}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin animalId responde 400 y no llega al caso de uso")
        void sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(post("/dewormings").contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"2026-04-01","type":"INTERNAL","product":"Drontal Plus",
                     "dosage":"1 tableta"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /dewormings")
    class Listado {

        @Test
        @DisplayName("devuelve la lista completa, sin paginar")
        void devuelve_la_lista_completa_sin_paginar() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(dto()));

            mockMvc.perform(get("/dewormings")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(DewormingMother.DEWORMING_ID));
        }
    }

    @Nested
    @DisplayName("GET /dewormings/by-animal/{animalId}")
    class ListadoPorAnimal {

        @Test
        @DisplayName("devuelve la pagina con el companyId del contexto")
        void devuelve_la_pagina_con_el_company_id_del_contexto() throws Exception {
            when(listByAnimalUseCase.listByAnimal(DewormingMother.ANIMAL_ID, COMPANY_ID, null, 0,
                    20)).thenReturn(new PageResult<>(List.of(dto()), 0, 20, 1L, 1));

            mockMvc.perform(get("/dewormings/by-animal/{animalId}", DewormingMother.ANIMAL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(DewormingMother.DEWORMING_ID))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /dewormings/{id}")
    class Busqueda {

        @Test
        @DisplayName("devuelve la desparasitacion encontrada")
        void devuelve_la_desparasitacion_encontrada() throws Exception {
            when(findUseCase.findById(DewormingMother.DEWORMING_ID, COMPANY_ID)).thenReturn(dto());

            mockMvc.perform(get("/dewormings/{id}", DewormingMother.DEWORMING_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.product").value("Drontal Plus"));
        }

        @Test
        @DisplayName("una desparasitacion inexistente responde 404, no 500")
        void una_desparasitacion_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new DewormingNotFoundException(99L));

            mockMvc.perform(get("/dewormings/{id}", 99L)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /dewormings/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 con la desparasitacion actualizada")
        void responde_200_con_la_desparasitacion_actualizada() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/dewormings/{id}", DewormingMother.DEWORMING_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZACION_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DewormingMother.DEWORMING_ID));
        }

        @Test
        @DisplayName("traduce el request y el id de la ruta al command, con el companyId del contexto")
        void traduce_el_request_y_el_id_de_la_ruta_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(dto());

            mockMvc.perform(put("/dewormings/{id}", DewormingMother.DEWORMING_ID)
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZACION_VALIDO));

            verify(updateUseCase).execute(new UpdateDewormingCommand(DewormingMother.DEWORMING_ID,
                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 2, 1), DewormingType.EXTERNAL,
                    "Bravecto", "1 comprimido", LocalDate.of(2026, 8, 1), "Control en un mes", 101L,
                    200L, COMPANY_ID));
        }

        @Test
        @DisplayName("con dosage vacio responde 400 y no llega al caso de uso")
        void con_dosage_vacio_responde_400() throws Exception {
            mockMvc.perform(put("/dewormings/{id}", DewormingMother.DEWORMING_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"date":"2026-05-01","type":"EXTERNAL","product":"Bravecto",
                             "dosage":"","animalId":101}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("DELETE /dewormings/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204_sin_cuerpo() throws Exception {
            mockMvc.perform(delete("/dewormings/{id}", DewormingMother.DEWORMING_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("el companyId no viene del request: lo pone el controller desde el contexto")
        void delega_en_el_caso_de_uso_con_la_empresa_del_contexto() throws Exception {
            mockMvc.perform(delete("/dewormings/{id}", DewormingMother.DEWORMING_ID));

            verify(deleteUseCase).execute(DewormingMother.DEWORMING_ID,
                    WebMvcSliceConfig.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("PATCH /dewormings/{id}/enable")
    class Reactivacion {

        @Test
        @DisplayName("responde 200 con la desparasitacion habilitada")
        void responde_200_con_la_desparasitacion_habilitada() throws Exception {
            when(reactivateUseCase.execute(DewormingMother.DEWORMING_ID, COMPANY_ID))
                    .thenReturn(dto());

            mockMvc.perform(patch("/dewormings/{id}/enable", DewormingMother.DEWORMING_ID))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("delega en el caso de uso con el id de la ruta y el companyId del contexto")
        void delega_en_el_caso_de_uso_con_el_company_id_del_contexto() throws Exception {
            when(reactivateUseCase.execute(DewormingMother.DEWORMING_ID, COMPANY_ID))
                    .thenReturn(dto());

            mockMvc.perform(patch("/dewormings/{id}/enable", DewormingMother.DEWORMING_ID));

            verify(reactivateUseCase).execute(DewormingMother.DEWORMING_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("una desparasitacion inexistente en la empresa responde 404, no 500")
        void una_desparasitacion_inexistente_en_la_empresa_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L, COMPANY_ID))
                    .thenThrow(new DewormingNotFoundException(99L));

            mockMvc.perform(patch("/dewormings/{id}/enable", 99L)).andExpect(status().isNotFound());
        }
    }
}
