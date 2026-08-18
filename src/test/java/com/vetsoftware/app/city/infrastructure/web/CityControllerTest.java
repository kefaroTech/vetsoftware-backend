package com.vetsoftware.app.city.infrastructure.web;

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

import com.vetsoftware.app.city.application.command.CreateCityCommand;
import com.vetsoftware.app.city.application.command.UpdateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.dto.StateSummaryDto;
import com.vetsoftware.app.city.application.port.in.CreateCityUseCase;
import com.vetsoftware.app.city.application.port.in.DeleteCityUseCase;
import com.vetsoftware.app.city.application.port.in.FindCityUseCase;
import com.vetsoftware.app.city.application.port.in.ListCitiesByStateUseCase;
import com.vetsoftware.app.city.application.port.in.ListCitiesUseCase;
import com.vetsoftware.app.city.application.port.in.ReactivateCityUseCase;
import com.vetsoftware.app.city.application.port.in.UpdateCityUseCase;
import com.vetsoftware.app.city.domain.CityHasActiveChildrenException;
import com.vetsoftware.app.city.domain.CityNotFoundException;
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
 * Rodaja HTTP del controller de ciudades: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * A diferencia de proveedores o categorias de producto, este catalogo no tiene
 * {@code companyId}: es global y solo lo administra SYSTEM
 * ({@code @PreAuthorize} en los puertos de entrada), asi que aqui no hay nada
 * que verificar sobre la empresa del contexto — esa autorizacion se prueba
 * aparte, no en esta rodaja con seguridad deshabilitada.
 */
@WebMvcTest(CityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CityController — contrato HTTP")
class CityControllerTest {

    private static final String CUERPO_VALIDO = """
            {"name":"Medellin","stateId":9,"daneCode":"05001"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCityUseCase createUseCase;
    @MockitoBean
    private UpdateCityUseCase updateUseCase;
    @MockitoBean
    private FindCityUseCase findUseCase;
    @MockitoBean
    private ListCitiesUseCase listUseCase;
    @MockitoBean
    private ListCitiesByStateUseCase listByStateUseCase;
    @MockitoBean
    private DeleteCityUseCase deleteUseCase;
    @MockitoBean
    private ReactivateCityUseCase reactivateUseCase;

    private static CityDto medellin() {
        return new CityDto(70L, "Medellin", new StateSummaryDto(9L, "Antioquia"), "05001",
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    private static CreateCityCommand comandoDeCreacionEsperado() {
        return new CreateCityCommand("Medellin", 9L, "05001");
    }

    @Nested
    @DisplayName("POST /cities")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el recurso creado")
        void responde_201_con_el_recurso_creado() throws Exception {
            when(createUseCase.execute(any())).thenReturn(medellin());

            mockMvc.perform(
                    post("/cities").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.name").value("Medellin"))
                    .andExpect(jsonPath("$.state.name").value("Antioquia"))
                    .andExpect(jsonPath("$.daneCode").value("05001"))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(medellin());

            mockMvc.perform(
                    post("/cities").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO));

            verify(createUseCase).execute(comandoDeCreacionEsperado());
        }

        @Test
        @DisplayName("nombre vacio responde 400 y no llega al caso de uso")
        void nombre_vacio_responde_400() throws Exception {
            mockMvc.perform(post("/cities").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"","stateId":9}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("nombre de mas de 100 caracteres responde 400")
        void nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/cities").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + "x".repeat(101) + "\",\"stateId\":9}"))
                    .andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("stateId nulo responde 400")
        void state_id_nulo_responde_400() throws Exception {
            mockMvc.perform(post("/cities").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Medellin"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("daneCode de mas de 5 caracteres responde 400")
        void dane_code_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/cities").contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"Medellin","stateId":9,"daneCode":"050011"}
                    """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un departamento inexistente sale como 400, no como 500")
        void departamento_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("State not found: 9"));

            mockMvc.perform(
                    post("/cities").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /cities lista el catalogo global completo")
        void get_lista_el_catalogo_global() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(medellin()));

            mockMvc.perform(get("/cities")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(70));
        }

        @Test
        @DisplayName("GET /states/{stateId}/cities lista las del departamento")
        void get_lista_las_del_departamento() throws Exception {
            when(listByStateUseCase.listByState(9L)).thenReturn(List.of(medellin()));

            mockMvc.perform(get("/states/9/cities")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(70))
                    .andExpect(jsonPath("$[0].state.id").value(9));
        }

        @Test
        @DisplayName("GET /cities/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(70L)).thenReturn(medellin());

            mockMvc.perform(get("/cities/70")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.daneCode").value("05001"));
        }

        @Test
        @DisplayName("GET /cities/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L)).thenThrow(new CityNotFoundException(99L));

            mockMvc.perform(get("/cities/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("escrituras sobre una ciudad existente")
    class Escrituras {

        @Test
        @DisplayName("PUT /cities/{id} responde 200 con el recurso actualizado")
        void put_responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(medellin());

            mockMvc.perform(put("/cities/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70));
        }

        @Test
        @DisplayName("PUT traduce el request al command con el id de la ruta")
        void put_traduce_el_request_al_command() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(medellin());

            mockMvc.perform(put("/cities/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            verify(updateUseCase).execute(new UpdateCityCommand(70L, "Medellin", 9L, "05001"));
        }

        @Test
        @DisplayName("PUT sobre una ciudad inexistente responde 404")
        void put_de_ciudad_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new CityNotFoundException(70L));

            mockMvc.perform(put("/cities/70").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /cities/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/cities/70")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(70L);
        }

        @Test
        @DisplayName("DELETE de una ciudad inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new CityNotFoundException(99L)).when(deleteUseCase).execute(99L);

            mockMvc.perform(delete("/cities/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE de una ciudad con propietarios activos responde 409")
        void delete_con_propietarios_activos_responde_409() throws Exception {
            doThrow(new CityHasActiveChildrenException(70L, "owner")).when(deleteUseCase)
                    .execute(70L);

            mockMvc.perform(delete("/cities/70")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("PATCH /cities/{id}/enable reactiva y responde 200")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(70L)).thenReturn(medellin());

            mockMvc.perform(patch("/cities/70/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(70))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de una ciudad inexistente responde 404")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L)).thenThrow(new CityNotFoundException(99L));

            mockMvc.perform(patch("/cities/99/enable")).andExpect(status().isNotFound());
        }
    }
}
