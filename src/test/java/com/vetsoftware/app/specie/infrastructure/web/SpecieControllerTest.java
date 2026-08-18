package com.vetsoftware.app.specie.infrastructure.web;

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

import com.vetsoftware.app.specie.application.command.CreateSpecieCommand;
import com.vetsoftware.app.specie.application.command.UpdateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.in.CreateSpecieUseCase;
import com.vetsoftware.app.specie.application.port.in.DeleteSpecieUseCase;
import com.vetsoftware.app.specie.application.port.in.FindSpecieUseCase;
import com.vetsoftware.app.specie.application.port.in.ListSpeciesUseCase;
import com.vetsoftware.app.specie.application.port.in.ReactivateSpecieUseCase;
import com.vetsoftware.app.specie.application.port.in.UpdateSpecieUseCase;
import com.vetsoftware.app.specie.domain.SpecieHasActiveChildrenException;
import com.vetsoftware.app.specie.domain.SpecieNotFoundException;
import com.vetsoftware.app.specie.testsupport.SpecieMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de {@link SpecieController}: rutas, binding, validacion del
 * request y codigos de estado. Lo que hay debajo son dobles.
 *
 * <p>
 * No verifica el {@code hasRole('SYSTEM')} de los puertos de entrada: la cadena
 * de seguridad real (Redis, JWT) se sustituye aqui por una permisiva, igual que
 * en {@code AnimalControllerTest} — esa frontera la cubre ArchUnit (todo puerto
 * de entrada con {@code @PreAuthorize}) y no una rodaja {@code @WebMvcTest}.
 */
@WebMvcTest(SpecieController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SpecieController — contrato HTTP")
class SpecieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSpecieUseCase createUseCase;
    @MockitoBean
    private UpdateSpecieUseCase updateUseCase;
    @MockitoBean
    private FindSpecieUseCase findUseCase;
    @MockitoBean
    private ListSpeciesUseCase listUseCase;
    @MockitoBean
    private DeleteSpecieUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSpecieUseCase reactivateUseCase;

    private static SpecieDto perro() {
        return SpecieDto.from(SpecieMother.perro());
    }

    @Nested
    @DisplayName("POST /species")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la especie creada")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(perro());

            mockMvc.perform(post("/species").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Perro"}
                    """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SpecieMother.SPECIE_ID))
                    .andExpect(jsonPath("$.name").value("Perro"));
        }

        @Test
        @DisplayName("el nombre del request llega intacto al comando")
        void el_nombre_del_request_llega_al_comando() throws Exception {
            when(createUseCase.execute(any())).thenReturn(perro());

            mockMvc.perform(post("/species").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Perro"}
                    """));

            verify(createUseCase).execute(new CreateSpecieCommand("Perro"));
        }

        @Test
        @DisplayName("sin nombre responde 400 y no crea nada")
        void sin_nombre_responde_400() throws Exception {
            mockMvc.perform(post("/species").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un nombre de mas de 100 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            String cuerpo = "{\"name\":\"" + "x".repeat(101) + "\"}";

            mockMvc.perform(
                    post("/species").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /species")
    class Listado {

        @Test
        @DisplayName("devuelve la lista completa sin envoltura de paginacion")
        void devuelve_la_lista_completa() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(perro()));

            mockMvc.perform(get("/species")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Perro"));
        }
    }

    @Nested
    @DisplayName("GET /species/{id}")
    class Busqueda {

        @Test
        @DisplayName("devuelve el detalle de la especie")
        void devuelve_el_detalle() throws Exception {
            when(findUseCase.findById(SpecieMother.SPECIE_ID)).thenReturn(perro());

            mockMvc.perform(get("/species/" + SpecieMother.SPECIE_ID)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SpecieMother.SPECIE_ID));
        }

        @Test
        @DisplayName("una especie inexistente responde 404, no 500")
        void una_especie_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L)).thenThrow(new SpecieNotFoundException(999L));

            mockMvc.perform(get("/species/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /species/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 con la especie actualizada")
        void responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(perro());

            mockMvc.perform(put("/species/" + SpecieMother.SPECIE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Perro"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Perro"));
        }

        @Test
        @DisplayName("el id sale de la ruta, nunca del cuerpo")
        void el_id_sale_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(perro());

            mockMvc.perform(put("/species/" + SpecieMother.SPECIE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"name":"Perro"}
                            """));

            verify(updateUseCase).execute(new UpdateSpecieCommand(SpecieMother.SPECIE_ID, "Perro"));
        }

        @Test
        @DisplayName("sin nombre responde 400 y no actualiza nada")
        void sin_nombre_responde_400() throws Exception {
            mockMvc.perform(put("/species/" + SpecieMother.SPECIE_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("actualizar una especie inexistente responde 404")
        void actualizar_una_especie_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new SpecieNotFoundException(999L));

            mockMvc.perform(put("/species/999").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Perro"}
                    """)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /species/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204() throws Exception {
            mockMvc.perform(delete("/species/" + SpecieMother.SPECIE_ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(SpecieMother.SPECIE_ID);
        }

        @Test
        @DisplayName("borrar una especie con hijos activos responde 409, no 500")
        void borrar_con_hijos_activos_responde_409() throws Exception {
            Mockito.doThrow(new SpecieHasActiveChildrenException(SpecieMother.SPECIE_ID, "breed"))
                    .when(deleteUseCase).execute(SpecieMother.SPECIE_ID);

            mockMvc.perform(delete("/species/" + SpecieMother.SPECIE_ID))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("borrar una especie inexistente responde 404")
        void borrar_una_especie_inexistente_responde_404() throws Exception {
            Mockito.doThrow(new SpecieNotFoundException(999L)).when(deleteUseCase).execute(999L);

            mockMvc.perform(delete("/species/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /species/{id}/enable")
    class Reactivacion {

        @Test
        @DisplayName("responde 200 con la especie reactivada")
        void responde_200() throws Exception {
            when(reactivateUseCase.execute(SpecieMother.SPECIE_ID)).thenReturn(perro());

            mockMvc.perform(patch("/species/" + SpecieMother.SPECIE_ID + "/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("reactivar una especie inexistente responde 404")
        void reactivar_una_especie_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(999L)).thenThrow(new SpecieNotFoundException(999L));

            mockMvc.perform(patch("/species/999/enable")).andExpect(status().isNotFound());
        }
    }
}
