package com.vetsoftware.app.baserole.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.command.UpdateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.CreateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.DeleteBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.FindBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.ListBaseRolesUseCase;
import com.vetsoftware.app.baserole.application.port.in.ReactivateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.UpdateBaseRoleUseCase;
import com.vetsoftware.app.baserole.domain.BaseRoleNotFoundException;
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

@WebMvcTest(BaseRoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BaseRoleController — contrato HTTP")
class BaseRoleControllerTest {

    private static final Long BASE_ROLE_ID = 1L;
    private static final String CREACION_VALIDA = """
            {"name":"Veterinario","code":"VET","mandatory":false}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBaseRoleUseCase createUseCase;
    @MockitoBean
    private UpdateBaseRoleUseCase updateUseCase;
    @MockitoBean
    private FindBaseRoleUseCase findUseCase;
    @MockitoBean
    private ListBaseRolesUseCase listUseCase;
    @MockitoBean
    private DeleteBaseRoleUseCase deleteUseCase;
    @MockitoBean
    private ReactivateBaseRoleUseCase reactivateUseCase;

    private static BaseRoleDto dto() {
        return new BaseRoleDto(BASE_ROLE_ID, "Veterinario", "VET", false,
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("create responde 201 con el rol creado")
    void create_responde_201_con_el_rol_creado() throws Exception {
        when(createUseCase.execute(any(CreateBaseRoleCommand.class))).thenReturn(dto());

        mockMvc.perform(post("/base-roles").contentType(MediaType.APPLICATION_JSON)
                .content(CREACION_VALIDA)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Veterinario"))
                .andExpect(jsonPath("$.code").value("VET"));
    }

    @Test
    @DisplayName("create con name en blanco responde 400")
    void create_con_name_en_blanco_responde_400() throws Exception {
        mockMvc.perform(post("/base-roles").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"","code":"VET","mandatory":false}
                """)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("listAll responde 200 con la lista")
    void list_all_responde_200_con_la_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(dto()));

        mockMvc.perform(get("/base-roles")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(BASE_ROLE_ID));
    }

    @Test
    @DisplayName("findById responde 200 con el rol")
    void find_by_id_responde_200_con_el_rol() throws Exception {
        when(findUseCase.findById(BASE_ROLE_ID)).thenReturn(dto());

        mockMvc.perform(get("/base-roles/{id}", BASE_ROLE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VET"));
    }

    @Test
    @DisplayName("findById con id inexistente responde 404")
    void find_by_id_con_id_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new BaseRoleNotFoundException(99L));

        mockMvc.perform(get("/base-roles/{id}", 99L)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update responde 200 con el rol actualizado")
    void update_responde_200_con_el_rol_actualizado() throws Exception {
        when(updateUseCase.execute(any(UpdateBaseRoleCommand.class))).thenReturn(dto());

        mockMvc.perform(put("/base-roles/{id}", BASE_ROLE_ID)
                .contentType(MediaType.APPLICATION_JSON).content(CREACION_VALIDA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("delete responde 204")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/base-roles/{id}", BASE_ROLE_ID)).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(BASE_ROLE_ID);
    }

    @Test
    @DisplayName("reactivate responde 200 con el rol reactivado")
    void reactivate_responde_200_con_el_rol_reactivado() throws Exception {
        when(reactivateUseCase.execute(BASE_ROLE_ID)).thenReturn(dto());

        mockMvc.perform(patch("/base-roles/{id}/enable", BASE_ROLE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
