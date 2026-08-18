package com.vetsoftware.app.rolepermission.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.command.SyncRolePermissionsCommand;
import com.vetsoftware.app.rolepermission.application.command.UpdateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.dto.RoleSummaryDto;
import com.vetsoftware.app.rolepermission.application.port.in.CreateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.DeleteRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.FindRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsByCompanyUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ReactivateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.SyncRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.UpdateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
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
 * Rodaja HTTP de la asignacion rol-permiso: rutas, binding, validacion del
 * request y forma del JSON. {@code create}/{@code syncByRole}/{@code update}/
 * {@code delete}/{@code reactivate} leen la empresa de
 * {@code authz.currentCompanyIdOrNull()} —no de
 * {@code WebMvcSliceConfig.authz()}, que solo deja stubeado
 * {@code currentCompanyId()}— asi que cada test que la necesita la resuelve
 * explicitamente.
 */
@WebMvcTest(RolePermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("RolePermissionController — contrato HTTP")
class RolePermissionControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long ROLE_PERMISSION_ID = 1L;
    private static final Long ROLE_ID = 3L;
    private static final Long PERMISSION_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateRolePermissionUseCase createUseCase;
    @MockitoBean
    private SyncRolePermissionsUseCase syncUseCase;
    @MockitoBean
    private UpdateRolePermissionUseCase updateUseCase;
    @MockitoBean
    private FindRolePermissionUseCase findUseCase;
    @MockitoBean
    private ListRolePermissionsUseCase listUseCase;
    @MockitoBean
    private ListRolePermissionsByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private DeleteRolePermissionUseCase deleteUseCase;
    @MockitoBean
    private ReactivateRolePermissionUseCase reactivateUseCase;

    private static RolePermissionDto asignacion() {
        return new RolePermissionDto(ROLE_PERMISSION_ID,
                new RoleSummaryDto(ROLE_ID, "Veterinario", "VET"),
                new PermissionSummaryDto(PERMISSION_ID, "Ver animales", "ANIMAL_READ"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Nested
    @DisplayName("POST /role-permissions")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la asignacion creada y la empresa la pone el backend")
        void responde_201_y_la_empresa_la_pone_el_backend() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(createUseCase.execute(any())).thenReturn(asignacion());

            mockMvc.perform(
                    post("/role-permissions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"roleId":3,"permissionId":7}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ROLE_PERMISSION_ID))
                    .andExpect(jsonPath("$.role.id").value(ROLE_ID))
                    .andExpect(jsonPath("$.role.code").value("VET"))
                    .andExpect(jsonPath("$.permission.id").value(PERMISSION_ID))
                    .andExpect(jsonPath("$.enabled").value(true));

            verify(createUseCase).execute(new CreateRolePermissionCommand(3L, 7L, COMPANY_ID));
        }

        @Test
        @DisplayName("sin roleId responde 400 y no crea nada")
        void sin_role_id_responde_400() throws Exception {
            mockMvc.perform(
                    post("/role-permissions").contentType(MediaType.APPLICATION_JSON).content("""
                            {"permissionId":7}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("PUT /role-permissions/by-role/{roleId}")
    class Sincronizacion {

        @Test
        @DisplayName("devuelve la lista reconciliada y arma el command con la empresa del contexto")
        void devuelve_la_lista_y_arma_el_command() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(syncUseCase.execute(any())).thenReturn(List.of(asignacion()));

            mockMvc.perform(put("/role-permissions/by-role/3")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"permissionIds":[7,8]}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(ROLE_PERMISSION_ID));

            verify(syncUseCase)
                    .execute(new SyncRolePermissionsCommand(3L, List.of(7L, 8L), COMPANY_ID));
        }

        @Test
        @DisplayName("sin permissionIds responde 400")
        void sin_permission_ids_responde_400() throws Exception {
            mockMvc.perform(put("/role-permissions/by-role/3")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());

            verify(syncUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un rol inexistente responde 400, no 500")
        void rol_inexistente_responde_400() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(syncUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Role not found: 3"));

            mockMvc.perform(put("/role-permissions/by-role/3")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"permissionIds":[7]}
                            """)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /role-permissions")
    class ListadoGlobal {

        @Test
        @DisplayName("lista todas las asignaciones, sin filtro de empresa")
        void lista_todas_las_asignaciones() throws Exception {
            when(listUseCase.listAll()).thenReturn(List.of(asignacion()));

            mockMvc.perform(get("/role-permissions")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].role.name").value("Veterinario"));
        }
    }

    @Nested
    @DisplayName("GET /role-permissions/by-company")
    class ListadoPorEmpresa {

        @Test
        @DisplayName("lista las asignaciones de la empresa resuelta por el contexto")
        void lista_las_asignaciones_de_la_empresa() throws Exception {
            when(listByCompanyUseCase.listByCompany(COMPANY_ID)).thenReturn(List.of(asignacion()));

            mockMvc.perform(get("/role-permissions/by-company")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].permission.code").value("ANIMAL_READ"));
        }
    }

    @Nested
    @DisplayName("GET /role-permissions/{id}")
    class Busqueda {

        @Test
        @DisplayName("responde 200 con la asignacion encontrada")
        void responde_200_con_la_asignacion() throws Exception {
            when(findUseCase.findById(ROLE_PERMISSION_ID)).thenReturn(asignacion());

            mockMvc.perform(get("/role-permissions/1")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ROLE_PERMISSION_ID));
        }

        @Test
        @DisplayName("una asignacion inexistente responde 404")
        void asignacion_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(ROLE_PERMISSION_ID))
                    .thenThrow(new RolePermissionNotFoundException(ROLE_PERMISSION_ID));

            mockMvc.perform(get("/role-permissions/1")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /role-permissions/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 y arma el command con el id de la ruta y la empresa del contexto")
        void responde_200_y_arma_el_command() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(updateUseCase.execute(any())).thenReturn(asignacion());

            mockMvc.perform(
                    put("/role-permissions/1").contentType(MediaType.APPLICATION_JSON).content("""
                            {"roleId":3,"permissionId":7}
                            """)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ROLE_PERMISSION_ID));

            verify(updateUseCase).execute(new UpdateRolePermissionCommand(1L, 3L, 7L, COMPANY_ID));
        }

        @Test
        @DisplayName("una asignacion de otra empresa responde 404")
        void asignacion_de_otra_empresa_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(updateUseCase.execute(any()))
                    .thenThrow(new RolePermissionNotFoundException(ROLE_PERMISSION_ID));

            mockMvc.perform(
                    put("/role-permissions/1").contentType(MediaType.APPLICATION_JSON).content("""
                            {"roleId":3,"permissionId":7}
                            """)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("sin permissionId responde 400 y no actualiza")
        void sin_permission_id_responde_400() throws Exception {
            mockMvc.perform(
                    put("/role-permissions/1").contentType(MediaType.APPLICATION_JSON).content("""
                            {"roleId":3}
                            """)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("DELETE y PATCH /role-permissions/{id}")
    class EstadoDeLaAsignacion {

        @Test
        @DisplayName("DELETE responde 204 y borra con el id y la empresa del contexto")
        void delete_responde_204() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);

            mockMvc.perform(delete("/role-permissions/1")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(ROLE_PERMISSION_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("PATCH /enable reactiva y responde 200 con la asignacion")
        void patch_enable_responde_200() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(reactivateUseCase.execute(ROLE_PERMISSION_ID, COMPANY_ID))
                    .thenReturn(asignacion());

            mockMvc.perform(patch("/role-permissions/1/enable")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("reactivar una asignacion inexistente responde 404")
        void reactivar_inexistente_responde_404() throws Exception {
            when(authz.currentCompanyIdOrNull()).thenReturn(COMPANY_ID);
            when(reactivateUseCase.execute(anyLong(), any()))
                    .thenThrow(new RolePermissionNotFoundException(ROLE_PERMISSION_ID));

            mockMvc.perform(patch("/role-permissions/1/enable")).andExpect(status().isNotFound());
        }
    }
}
