package com.vetsoftware.app.spatype.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.spatype.application.command.CreateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.command.UpdateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.CreateSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.in.DeleteSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.in.FindSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.in.ListSpaTypesUseCase;
import com.vetsoftware.app.spatype.application.port.in.ReactivateSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.in.UpdateSpaTypeUseCase;
import com.vetsoftware.app.spatype.domain.SpaTypeHasActiveChildrenException;
import com.vetsoftware.app.spatype.domain.SpaTypeNameAlreadyExistsException;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP de {@code SpaTypeController}: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo de debajo son dobles de los
 * puertos {@code port/in} — aqui no se prueba el caso de uso, se prueba el
 * contrato que ve el front y que {@code api/openapi.json} promete.
 *
 * <p>
 * Primer fichero de test del slice {@code spatype} junto con
 * {@code SpaTypePersistenceIT} (#426).
 */
@WebMvcTest(SpaTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SpaTypeController — contrato HTTP")
class SpaTypeControllerTest {

    private static final LocalDateTime CREADO = LocalDateTime.of(2026, 8, 23, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSpaTypeUseCase createUseCase;
    @MockitoBean
    private UpdateSpaTypeUseCase updateUseCase;
    @MockitoBean
    private FindSpaTypeUseCase findUseCase;
    @MockitoBean
    private ListSpaTypesUseCase listUseCase;
    @MockitoBean
    private DeleteSpaTypeUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSpaTypeUseCase reactivateUseCase;

    private static SpaTypeDto banoMedicado() {
        return new SpaTypeDto(1L, "Baño medicado", "Baño con champú medicado", CREADO, true);
    }

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        @DisplayName("POST /spa-types responde 201 con el recurso creado entero")
        void post_responde_201_con_el_recurso() throws Exception {
            when(createUseCase.execute(any())).thenReturn(banoMedicado());

            mockMvc.perform(post("/spa-types").contentType(MediaType.APPLICATION_JSON).content(
                    "{\"name\":\"Baño medicado\",\"description\":\"Baño con champú medicado\"}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Baño medicado"))
                    .andExpect(jsonPath("$.description").value("Baño con champú medicado"))
                    .andExpect(jsonPath("$.createdDate").exists())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("POST /spa-types traduce el request al command sin inventarse campos")
        void post_traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(banoMedicado());

            mockMvc.perform(post("/spa-types").contentType(MediaType.APPLICATION_JSON).content(
                    "{\"name\":\"Baño medicado\",\"description\":\"Baño con champú medicado\"}"));

            verify(createUseCase)
                    .execute(new CreateSpaTypeCommand("Baño medicado", "Baño con champú medicado"));
        }

        /**
         * El choque de nombre tiene que salir como 409 y no como 500. Antes de #559 la
         * guarda no existia y el choque lo detectaba solo la base: llegaba al handler
         * como violacion de constraint y salia con un mensaje en ingles y sin codigo de
         * negocio. Lo que se prueba aqui es que la excepcion de dominio nueva esta
         * REGISTRADA en el {@code GlobalExceptionHandler} — si alguien la crea y olvida
         * mapearla, el front recibe un 500 y este caso lo delata.
         */
        @Test
        @DisplayName("POST /spa-types con un nombre ya usado responde 409, no 500")
        void post_con_nombre_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new SpaTypeNameAlreadyExistsException("Baño medicado"));

            mockMvc.perform(post("/spa-types").contentType(MediaType.APPLICATION_JSON).content(
                    "{\"name\":\"Baño medicado\",\"description\":\"Baño con champú medicado\"}"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Validaciones del cuerpo")
    class Validaciones {

        @Test
        @DisplayName("POST con nombre en blanco responde 400 y no llega al caso de uso")
        void post_con_nombre_en_blanco_responde_400() throws Exception {
            mockMvc.perform(post("/spa-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\",\"description\":\"Da igual\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("POST con nombre de más de 100 caracteres responde 400")
        void post_con_nombre_demasiado_largo_responde_400() throws Exception {
            mockMvc.perform(post("/spa-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + "x".repeat(101) + "\",\"description\":\"Da igual\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        @Test
        @DisplayName("POST con descripción de más de 500 caracteres responde 400")
        void post_con_descripcion_demasiado_larga_responde_400() throws Exception {
            mockMvc.perform(post("/spa-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Baño\",\"description\":\"" + "x".repeat(501) + "\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(createUseCase);
        }

        /**
         * <b>Divergencia con el gemelo, deliberadamente afirmada.</b>
         * {@code CreateConsultationTypeRequest} —mismo CRUD, mismo tamano, escrito con
         * el mismo patron— exige {@code @NotBlank} en {@code description}; el de
         * {@code spatype} solo mide la longitud. La columna
         * {@code spa_types.description} es {@code NOT NULL}, asi que este 201 acaba en
         * el {@code DataIntegrityViolationException} que fija
         * {@code SpaTypePersistenceIT}. Este test fija el comportamiento REAL de hoy,
         * no el deseable: si alguien anade el {@code @NotBlank}, este caso falla y le
         * obliga a decidir a conciencia. Queda registrado como issue.
         */
        @Test
        @DisplayName("POST sin descripción NO se rechaza hoy: llega al caso de uso con null")
        void post_sin_descripcion_llega_al_caso_de_uso_con_null() throws Exception {
            when(createUseCase.execute(any())).thenReturn(banoMedicado());

            mockMvc.perform(post("/spa-types").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Baño sin descripción\"}"))
                    .andExpect(status().isCreated());

            ArgumentCaptor<CreateSpaTypeCommand> command = ArgumentCaptor
                    .forClass(CreateSpaTypeCommand.class);
            verify(createUseCase).execute(command.capture());
            assertThat(command.getValue().description()).isNull();
        }

        @Test
        @DisplayName("PUT con nombre en blanco responde 400 y no llega al caso de uso")
        void put_con_nombre_en_blanco_responde_400() throws Exception {
            mockMvc.perform(put("/spa-types/1").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"\",\"description\":\"Da igual\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(updateUseCase);
        }
    }

    @Nested
    @DisplayName("Lectura")
    class Lectura {

        @Test
        @DisplayName("GET /spa-types devuelve la lista completa")
        void get_lista() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(banoMedicado()));

            mockMvc.perform(get("/spa-types")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Baño medicado"));
        }

        @Test
        @DisplayName("GET /spa-types con el catálogo vacío devuelve [] y no null")
        void get_lista_vacia_devuelve_array_vacio() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/spa-types")).andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("GET /spa-types/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(1L)).thenReturn(banoMedicado());

            mockMvc.perform(get("/spa-types/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Baño medicado"));
        }

        @Test
        @DisplayName("GET /spa-types/{id} inexistente responde 404, no 500")
        void get_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L)).thenThrow(new SpaTypeNotFoundException(99L));

            mockMvc.perform(get("/spa-types/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Edición, baja y reactivación")
    class EdicionBajaYReactivacion {

        @Test
        @DisplayName("PUT /spa-types/{id} responde 200 y lleva el id de la ruta al command")
        void put_responde_200_y_lleva_el_id_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(
                    new SpaTypeDto(7L, "Nuevo nombre", "Nueva descripción", CREADO, true));

            mockMvc.perform(put("/spa-types/7").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Nuevo nombre\",\"description\":\"Nueva descripción\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Nuevo nombre"));

            verify(updateUseCase)
                    .execute(new UpdateSpaTypeCommand(7L, "Nuevo nombre", "Nueva descripción"));
        }

        @Test
        @DisplayName("PUT /spa-types/{id} con un nombre ya usado responde 409, no 500")
        void put_con_nombre_repetido_responde_409() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenThrow(new SpaTypeNameAlreadyExistsException("Baño medicado"));

            mockMvc.perform(put("/spa-types/7").contentType(MediaType.APPLICATION_JSON).content(
                    "{\"name\":\"Baño medicado\",\"description\":\"Baño con champú medicado\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE /spa-types/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/spa-types/1")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(1L);
        }

        @Test
        @DisplayName("DELETE de un tipo con spas activos no responde 500")
        void delete_con_hijos_activos_no_responde_500() throws Exception {
            org.mockito.Mockito.doThrow(new SpaTypeHasActiveChildrenException(1L, "spa"))
                    .when(deleteUseCase).execute(1L);

            mockMvc.perform(delete("/spa-types/1")).andExpect(
                    result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(500));
        }

        @Test
        @DisplayName("PATCH /spa-types/{id}/enable responde 200 con el recurso habilitado")
        void patch_enable_responde_200() throws Exception {
            when(reactivateUseCase.execute(1L)).thenReturn(banoMedicado());

            mockMvc.perform(patch("/spa-types/1/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("PATCH enable de un id inexistente responde 404, no 500")
        void patch_enable_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(99L)).thenThrow(new SpaTypeNotFoundException(99L));

            mockMvc.perform(patch("/spa-types/99/enable")).andExpect(status().isNotFound());

            verify(reactivateUseCase).execute(99L);
            verify(findUseCase, never()).findById(any());
        }
    }
}
