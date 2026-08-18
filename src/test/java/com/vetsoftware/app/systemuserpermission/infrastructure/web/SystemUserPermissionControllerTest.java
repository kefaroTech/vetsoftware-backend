package com.vetsoftware.app.systemuserpermission.infrastructure.web;

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

import com.vetsoftware.app.systemuserpermission.application.command.CreateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.command.UpdateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemPermissionSummaryDto;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserSummaryDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.CreateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.DeleteSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.FindSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.ListSystemUserPermissionsUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.ReactivateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.UpdateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
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
@WebMvcTest(SystemUserPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemUserPermissionController — contrato HTTP")
class SystemUserPermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSystemUserPermissionUseCase createUseCase;
    @MockitoBean
    private UpdateSystemUserPermissionUseCase updateUseCase;
    @MockitoBean
    private FindSystemUserPermissionUseCase findUseCase;
    @MockitoBean
    private ListSystemUserPermissionsUseCase listUseCase;
    @MockitoBean
    private DeleteSystemUserPermissionUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSystemUserPermissionUseCase reactivateUseCase;

    private static SystemUserPermissionDto asignacion() {
        return new SystemUserPermissionDto(1L, new SystemUserSummaryDto(5L, "admin-api"),
                new SystemPermissionSummaryDto(8L, "Gestionar Reportes", "reports.manage"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /system-user-permissions responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(asignacion());

        mockMvc.perform(post("/system-user-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"systemUserId\":5,\"systemPermissionId\":8}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.systemUser.code").value("admin-api"))
                .andExpect(jsonPath("$.systemPermission.name").value("Gestionar Reportes"));
    }

    @Test
    @DisplayName("POST /system-user-permissions traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(asignacion());

        mockMvc.perform(post("/system-user-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"systemUserId\":5,\"systemPermissionId\":8}"));

        verify(createUseCase).execute(new CreateSystemUserPermissionCommand(5L, 8L));
    }

    @Test
    @DisplayName("POST /system-user-permissions sin systemUserId responde 400 y no llega al caso de uso")
    void post_sin_system_user_id_responde_400() throws Exception {
        mockMvc.perform(post("/system-user-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"systemPermissionId\":8}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /system-user-permissions devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(asignacion()));

        mockMvc.perform(get("/system-user-permissions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /system-user-permissions/{id} devuelve la asignacion")
    void get_por_id() throws Exception {
        when(findUseCase.findById(1L)).thenReturn(asignacion());

        mockMvc.perform(get("/system-user-permissions/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPermission.code").value("reports.manage"));
    }

    @Test
    @DisplayName("GET /system-user-permissions/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new SystemUserPermissionNotFoundException(99L));

        mockMvc.perform(get("/system-user-permissions/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /system-user-permissions/{id} responde 200 y traduce el id de la ruta")
    void put_responde_200_y_traduce_el_id() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(asignacion());

        mockMvc.perform(put("/system-user-permissions/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"systemUserId\":5,\"systemPermissionId\":8}"))
                .andExpect(status().isOk());

        verify(updateUseCase).execute(new UpdateSystemUserPermissionCommand(1L, 5L, 8L));
    }

    @Test
    @DisplayName("DELETE /system-user-permissions/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/system-user-permissions/1")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(1L);
    }

    @Test
    @DisplayName("PATCH /system-user-permissions/{id}/enable responde 200 con el recurso reactivado")
    void patch_enable_responde_200() throws Exception {
        when(reactivateUseCase.execute(1L)).thenReturn(asignacion());

        mockMvc.perform(patch("/system-user-permissions/1/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(1L);
    }
}
