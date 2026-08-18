package com.vetsoftware.app.systempermission.infrastructure.web;

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

import com.vetsoftware.app.systempermission.application.command.CreateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.in.CreateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.in.DeleteSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.in.FindSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.in.ListSystemPermissionsUseCase;
import com.vetsoftware.app.systempermission.application.port.in.ReactivateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.in.UpdateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.domain.SystemPermissionNotFoundException;
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

@WebMvcTest(SystemPermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SystemPermissionController — contrato HTTP")
class SystemPermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSystemPermissionUseCase createUseCase;
    @MockitoBean
    private UpdateSystemPermissionUseCase updateUseCase;
    @MockitoBean
    private FindSystemPermissionUseCase findUseCase;
    @MockitoBean
    private ListSystemPermissionsUseCase listUseCase;
    @MockitoBean
    private DeleteSystemPermissionUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSystemPermissionUseCase reactivateUseCase;

    private static SystemPermissionDto adminUsers() {
        return new SystemPermissionDto(1L, "Administrar usuarios", "admin.users",
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /system-permissions responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(adminUsers());

        mockMvc.perform(post("/system-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Administrar usuarios\",\"code\":\"admin.users\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Administrar usuarios"))
                .andExpect(jsonPath("$.code").value("admin.users"));
    }

    @Test
    @DisplayName("POST /system-permissions traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(adminUsers());

        mockMvc.perform(post("/system-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Administrar usuarios\",\"code\":\"admin.users\"}"));

        verify(createUseCase)
                .execute(new CreateSystemPermissionCommand("Administrar usuarios", "admin.users"));
    }

    @Test
    @DisplayName("POST /system-permissions con name vacio responde 400 y no llega al caso de uso")
    void post_con_name_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/system-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"code\":\"admin.users\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /system-permissions devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(adminUsers()));

        mockMvc.perform(get("/system-permissions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /system-permissions/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new SystemPermissionNotFoundException(99L));

        mockMvc.perform(get("/system-permissions/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /system-permissions/{id} existente responde 200 con el recurso encontrado")
    void get_existente_responde_200() throws Exception {
        when(findUseCase.findById(1L)).thenReturn(adminUsers());

        mockMvc.perform(get("/system-permissions/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Administrar usuarios"));
    }

    @Test
    @DisplayName("PUT /system-permissions/{id} responde 200")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(adminUsers());

        mockMvc.perform(put("/system-permissions/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Administrar usuarios\",\"code\":\"admin.users\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /system-permissions/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/system-permissions/1")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(1L);
    }

    @Test
    @DisplayName("PATCH /system-permissions/{id}/enable responde 200 con el permiso reactivado")
    void patch_enable_responde_200() throws Exception {
        when(reactivateUseCase.execute(1L)).thenReturn(adminUsers());

        mockMvc.perform(patch("/system-permissions/1/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(1L);
    }
}
