package com.vetsoftware.app.systemuser.infrastructure.web;

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

import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.CreateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.DeleteSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.FindSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.ListSystemUsersUseCase;
import com.vetsoftware.app.systemuser.application.port.in.ReactivateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.UpdateSystemUserUseCase;
import com.vetsoftware.app.systemuser.domain.SystemUserHasActiveChildrenException;
import com.vetsoftware.app.systemuser.domain.SystemUserNotFoundException;
import com.vetsoftware.app.systemuser.testsupport.SystemUserMother;
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
 * Rodaja HTTP de {@link SystemUserController}: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * A diferencia de otros controllers del proyecto, este recurso es GLOBAL de
 * plataforma: los puertos exigen {@code hasRole('SYSTEM')} a secas, sin
 * {@code companyId} de por medio. No hay nada que sellar desde el contexto de
 * tenant, asi que ningun test de esta clase comprueba un companyId inyectado.
 */
@WebMvcTest(SystemUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemUserController — contrato HTTP")
class SystemUserControllerTest {

    private static final String ALTA_VALIDA = """
            {"code":"svc-integracion","password":"unaContrasenaSegura1"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSystemUserUseCase createUseCase;
    @MockitoBean
    private UpdateSystemUserUseCase updateUseCase;
    @MockitoBean
    private FindSystemUserUseCase findUseCase;
    @MockitoBean
    private ListSystemUsersUseCase listUseCase;
    @MockitoBean
    private DeleteSystemUserUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSystemUserUseCase reactivateUseCase;

    private static SystemUserDto activo() {
        return SystemUserDto.from(SystemUserMother.activo());
    }

    @Nested
    @DisplayName("POST /system-users")
    class Creacion {

        @Test
        @DisplayName("responde 201 con el usuario creado")
        void responde_201() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(post("/system-users").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SystemUserMother.SYSTEM_USER_ID))
                    .andExpect(jsonPath("$.code").value(SystemUserMother.CODE))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("mapea el request al command tal cual")
        void mapea_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(post("/system-users").contentType(MediaType.APPLICATION_JSON)
                    .content(ALTA_VALIDA));

            verify(createUseCase).execute(
                    new CreateSystemUserCommand("svc-integracion", "unaContrasenaSegura1"));
        }

        @Test
        @DisplayName("sin code responde 400 y no crea nada")
        void sin_code_responde_400() throws Exception {
            mockMvc.perform(
                    post("/system-users").contentType(MediaType.APPLICATION_JSON).content("""
                            {"password":"unaContrasenaSegura1"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una contrasena de menos de 8 chars responde 400 y no crea nada")
        void password_corta_responde_400() throws Exception {
            mockMvc.perform(
                    post("/system-users").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"svc-integracion","password":"corta"}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("GET /system-users")
    class Listado {

        @Test
        @DisplayName("devuelve la lista de usuarios de sistema")
        void devuelve_la_lista() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(activo()));

            mockMvc.perform(get("/system-users")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].code").value(SystemUserMother.CODE));
        }
    }

    @Nested
    @DisplayName("GET /system-users/{id}")
    class Busqueda {

        @Test
        @DisplayName("devuelve el detalle del usuario")
        void devuelve_el_detalle() throws Exception {
            when(findUseCase.findById(SystemUserMother.SYSTEM_USER_ID)).thenReturn(activo());

            mockMvc.perform(get("/system-users/" + SystemUserMother.SYSTEM_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SystemUserMother.SYSTEM_USER_ID));
        }

        @Test
        @DisplayName("un usuario inexistente responde 404, no 500")
        void un_usuario_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(999L)).thenThrow(new SystemUserNotFoundException(999L));

            mockMvc.perform(get("/system-users/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /system-users/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 con el usuario actualizado")
        void responde_200() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(put("/system-users/" + SystemUserMother.SYSTEM_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"svc-actualizado"}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(SystemUserMother.CODE));
        }

        @Test
        @DisplayName("el id sale de la ruta, nunca del cuerpo")
        void el_id_sale_de_la_ruta() throws Exception {
            when(updateUseCase.execute(any())).thenReturn(activo());

            mockMvc.perform(put("/system-users/" + SystemUserMother.SYSTEM_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"svc-actualizado"}
                            """));

            verify(updateUseCase).execute(new UpdateSystemUserCommand(
                    SystemUserMother.SYSTEM_USER_ID, "svc-actualizado"));
        }

        @Test
        @DisplayName("sin code responde 400 y no actualiza nada")
        void sin_code_responde_400() throws Exception {
            mockMvc.perform(put("/system-users/" + SystemUserMother.SYSTEM_USER_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("actualizar un usuario inexistente responde 404")
        void actualizar_un_usuario_inexistente_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new SystemUserNotFoundException(999L));

            mockMvc.perform(
                    put("/system-users/999").contentType(MediaType.APPLICATION_JSON).content("""
                            {"code":"svc-actualizado"}
                            """)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /system-users/{id}")
    class Borrado {

        @Test
        @DisplayName("responde 204 sin cuerpo")
        void responde_204() throws Exception {
            mockMvc.perform(delete("/system-users/" + SystemUserMother.SYSTEM_USER_ID))
                    .andExpect(status().isNoContent());

            verify(deleteUseCase).execute(SystemUserMother.SYSTEM_USER_ID);
        }

        @Test
        @DisplayName("borrar un usuario con permisos de sistema activos responde 409, no 500")
        void borrar_con_permisos_activos_responde_409() throws Exception {
            doThrow(new SystemUserHasActiveChildrenException(SystemUserMother.SYSTEM_USER_ID,
                    "systemUserPermission")).when(deleteUseCase)
                    .execute(SystemUserMother.SYSTEM_USER_ID);

            mockMvc.perform(delete("/system-users/" + SystemUserMother.SYSTEM_USER_ID))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("borrar un usuario inexistente responde 404")
        void borrar_un_usuario_inexistente_responde_404() throws Exception {
            doThrow(new SystemUserNotFoundException(999L)).when(deleteUseCase).execute(999L);

            mockMvc.perform(delete("/system-users/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /system-users/{id}/enable")
    class Reactivacion {

        @Test
        @DisplayName("responde 200 con el usuario reactivado")
        void responde_200() throws Exception {
            when(reactivateUseCase.execute(SystemUserMother.SYSTEM_USER_ID)).thenReturn(activo());

            mockMvc.perform(patch("/system-users/" + SystemUserMother.SYSTEM_USER_ID + "/enable"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("reactivar un usuario inexistente responde 404")
        void reactivar_un_usuario_inexistente_responde_404() throws Exception {
            when(reactivateUseCase.execute(999L)).thenThrow(new SystemUserNotFoundException(999L));

            mockMvc.perform(patch("/system-users/999/enable")).andExpect(status().isNotFound());
        }
    }
}
