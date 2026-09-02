package com.vetsoftware.app.baserolepermission.infrastructure.web;

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

import com.vetsoftware.app.baserolepermission.application.command.CreateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BasePermissionSummaryDto;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRoleSummaryDto;
import com.vetsoftware.app.baserolepermission.application.port.in.CreateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.DeleteBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.FindBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.ListBaseRolePermissionsUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.ReactivateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.UpdateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermissionNotFoundException;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BaseRolePermissionController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BaseRolePermissionController — contrato HTTP")
class BaseRolePermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBaseRolePermissionUseCase createUseCase;
    @MockitoBean
    private UpdateBaseRolePermissionUseCase updateUseCase;
    @MockitoBean
    private FindBaseRolePermissionUseCase findUseCase;
    @MockitoBean
    private ListBaseRolePermissionsUseCase listUseCase;
    @MockitoBean
    private DeleteBaseRolePermissionUseCase deleteUseCase;
    @MockitoBean
    private ReactivateBaseRolePermissionUseCase reactivateUseCase;

    private static BaseRolePermissionDto vinculo() {
        return new BaseRolePermissionDto(2L, new BaseRoleSummaryDto(1L, "Veterinario", "VET"),
                new BasePermissionSummaryDto(10L, "Crear consulta", "CONSULTA_CREATE"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /base-role-permissions responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(vinculo());

        mockMvc.perform(post("/base-role-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"baseRoleId\":1,\"basePermissionId\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.baseRole.code").value("VET"))
                .andExpect(jsonPath("$.basePermission.code").value("CONSULTA_CREATE"));
    }

    @Test
    @DisplayName("POST /base-role-permissions traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(vinculo());

        mockMvc.perform(post("/base-role-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"baseRoleId\":1,\"basePermissionId\":10}"));

        verify(createUseCase).execute(new CreateBaseRolePermissionCommand(1L, 10L));
    }

    @Test
    @DisplayName("POST /base-role-permissions sin baseRoleId responde 400 y no llega al caso de uso")
    void post_sin_base_role_id_responde_400() throws Exception {
        mockMvc.perform(post("/base-role-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"basePermissionId\":10}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /base-role-permissions devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(vinculo()));

        mockMvc.perform(get("/base-role-permissions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /base-role-permissions/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new BaseRolePermissionNotFoundException(99L));

        mockMvc.perform(get("/base-role-permissions/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /base-role-permissions/{id} responde 200")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(vinculo());

        mockMvc.perform(put("/base-role-permissions/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"baseRoleId\":1,\"basePermissionId\":10}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /base-role-permissions/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/base-role-permissions/2")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(2L);
    }

    @Test
    @DisplayName("PATCH /base-role-permissions/{id}/enable responde 200 con el vinculo reactivado")
    void patch_enable_responde_200_con_el_vinculo_reactivado() throws Exception {
        when(reactivateUseCase.execute(2L)).thenReturn(vinculo());

        mockMvc.perform(patch("/base-role-permissions/2/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(2L);
    }
}
