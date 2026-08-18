package com.vetsoftware.app.animalalert.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.animalalert.application.command.CreateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.command.UpdateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.in.CreateAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.port.in.DeleteAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.port.in.ListAnimalAlertsByAnimalUseCase;
import com.vetsoftware.app.animalalert.application.port.in.UpdateAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.query.ListAnimalAlertsByAnimalQuery;
import com.vetsoftware.app.animalalert.domain.AlertSeverity;
import com.vetsoftware.app.animalalert.domain.AlertType;
import com.vetsoftware.app.animalalert.domain.AnimalAlertNotFoundException;
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
 * Rodaja HTTP del controller de alertas clinicas: rutas, binding, validacion
 * del request, codigos de estado y forma del JSON. Lo que hay debajo son
 * dobles.
 *
 * <p>
 * La empresa nunca viaja en el cuerpo: ningun request de alertas lleva
 * {@code companyId}, lo pone {@code Authz}. Cada traduccion request->command
 * comprueba que el command sale sellado con
 * {@link WebMvcSliceConfig#COMPANY_ID}.
 */
@WebMvcTest(AnimalAlertController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("AnimalAlertController — contrato HTTP")
class AnimalAlertControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long ANIMAL_ID = 100L;
    private static final Long ALERT_ID = 500L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateAnimalAlertUseCase createUseCase;
    @MockitoBean
    private UpdateAnimalAlertUseCase updateUseCase;
    @MockitoBean
    private DeleteAnimalAlertUseCase deleteUseCase;
    @MockitoBean
    private ListAnimalAlertsByAnimalUseCase listByAnimalUseCase;

    private static AnimalAlertDto alertaDto() {
        return new AnimalAlertDto(ALERT_ID, ANIMAL_ID, "Firulais", AlertType.ALLERGY,
                "Alergia a la penicilina", AlertSeverity.HIGH,
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("POST /animal-alerts responde 201 y sella la empresa desde el contexto")
        void post_responde_201_y_sella_la_empresa() throws Exception {
            when(createUseCase.execute(any())).thenReturn(alertaDto());

            mockMvc.perform(
                    post("/animal-alerts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":100,"type":"ALLERGY",
                             "description":"Alergia a la penicilina","severity":"HIGH"}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.animalName").value("Firulais"))
                    .andExpect(jsonPath("$.type").value("ALLERGY"));

            verify(createUseCase).execute(new CreateAnimalAlertCommand(ANIMAL_ID, AlertType.ALLERGY,
                    "Alergia a la penicilina", AlertSeverity.HIGH, COMPANY_ID));
        }

        @Test
        @DisplayName("POST /animal-alerts sin descripcion responde 400 y no llama al caso de uso")
        void post_sin_descripcion_responde_400() throws Exception {
            mockMvc.perform(
                    post("/animal-alerts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":100,"type":"ALLERGY"}
                            """)).andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("POST /animal-alerts sin animalId responde 400")
        void post_sin_animal_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/animal-alerts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"ALLERGY","description":"Alergia"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un animal inexistente responde 400, no 500")
        void un_animal_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Animal not found: 100"));

            mockMvc.perform(
                    post("/animal-alerts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"animalId":100,"type":"ALLERGY","description":"Alergia"}
                            """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("listado por animal")
    class ListadoPorAnimal {

        @Test
        @DisplayName("GET /animal-alerts/by-animal/{animalId} devuelve la lista con la empresa del contexto")
        void get_devuelve_la_lista_con_la_empresa_del_contexto() throws Exception {
            when(listByAnimalUseCase.execute(any())).thenReturn(List.of(alertaDto()));

            mockMvc.perform(get("/animal-alerts/by-animal/100")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(500))
                    .andExpect(jsonPath("$[0].description").value("Alergia a la penicilina"));

            verify(listByAnimalUseCase)
                    .execute(new ListAnimalAlertsByAnimalQuery(ANIMAL_ID, COMPANY_ID));
        }

        @Test
        @DisplayName("un animal sin alertas devuelve una lista vacia")
        void un_animal_sin_alertas_devuelve_lista_vacia() throws Exception {
            when(listByAnimalUseCase.execute(any())).thenReturn(List.of());

            mockMvc.perform(get("/animal-alerts/by-animal/100")).andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("PUT /animal-alerts/{id} sella la empresa desde el contexto")
        void put_sella_la_empresa_desde_el_contexto() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(alertaDto());

            mockMvc.perform(
                    put("/animal-alerts/500").contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"BEHAVIOR","description":"Agresivo con extranos",
                             "severity":"MEDIUM"}
                            """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(500));

            verify(updateUseCase).execute(new UpdateAnimalAlertCommand(ALERT_ID, AlertType.BEHAVIOR,
                    "Agresivo con extranos", AlertSeverity.MEDIUM, COMPANY_ID));
        }

        @Test
        @DisplayName("PUT /animal-alerts/{id} sin tipo responde 400")
        void put_sin_tipo_responde_400() throws Exception {
            mockMvc.perform(
                    put("/animal-alerts/500").contentType(MediaType.APPLICATION_JSON).content("""
                            {"description":"Agresivo"}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una alerta inexistente responde 404, no 500")
        void una_alerta_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new AnimalAlertNotFoundException(500L));

            mockMvc.perform(
                    put("/animal-alerts/500").contentType(MediaType.APPLICATION_JSON).content("""
                            {"type":"BEHAVIOR","description":"Agresivo"}
                            """)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("DELETE /animal-alerts/{id} responde 204 con la empresa del contexto")
        void delete_responde_204_con_la_empresa_del_contexto() throws Exception {
            mockMvc.perform(delete("/animal-alerts/500")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(ALERT_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("borrar una alerta inexistente responde 404, no 500")
        void borrar_una_alerta_inexistente_responde_404() throws Exception {
            org.mockito.Mockito.doThrow(new AnimalAlertNotFoundException(500L)).when(deleteUseCase)
                    .execute(ALERT_ID, COMPANY_ID);

            mockMvc.perform(delete("/animal-alerts/500")).andExpect(status().isNotFound());
        }
    }
}
