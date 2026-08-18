package com.vetsoftware.app.role.infrastructure.web;

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

import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.dto.CompanySummaryDto;
import com.vetsoftware.app.role.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.CreateRoleUseCase;
import com.vetsoftware.app.role.application.port.in.DeleteRoleUseCase;
import com.vetsoftware.app.role.application.port.in.FindRoleUseCase;
import com.vetsoftware.app.role.application.port.in.ListRolesByCompanyUseCase;
import com.vetsoftware.app.role.application.port.in.ListRolesUseCase;
import com.vetsoftware.app.role.application.port.in.ReactivateRoleUseCase;
import com.vetsoftware.app.role.application.port.in.UpdateRoleUseCase;
import com.vetsoftware.app.role.domain.RoleNotFoundException;
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

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("RoleController — contrato HTTP")
class RoleControllerTest {

    private static final Long ROLE_ID = 1L;
    private static final String CREACION_VALIDA = """
            {"name":"Veterinario","code":"VET"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateRoleUseCase createUseCase;
    @MockitoBean
    private UpdateRoleUseCase updateUseCase;
    @MockitoBean
    private FindRoleUseCase findUseCase;
    @MockitoBean
    private ListRolesUseCase listUseCase;
    @MockitoBean
    private ListRolesByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private DeleteRoleUseCase deleteUseCase;
    @MockitoBean
    private ReactivateRoleUseCase reactivateUseCase;

    private static RoleDto dto() {
        return new RoleDto(ROLE_ID, "Veterinario", "VET",
                new CompanySummaryDto(WebMvcSliceConfig.COMPANY_ID, "Clinica Norte", "NIT-900"),
                LocalDateTime.of(2026, 1, 15, 10, 30),
                List.of(new PermissionSummaryDto(10L, 20L, "Ver animales", "ANIMAL_READ")), true);
    }

    @Test
    @DisplayName("create responde 201 con el rol creado")
    void create_responde_201_con_el_rol_creado() throws Exception {
        when(createUseCase.execute(any(CreateRoleCommand.class))).thenReturn(dto());

        mockMvc.perform(
                post("/roles").contentType(MediaType.APPLICATION_JSON).content(CREACION_VALIDA))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Veterinario"))
                .andExpect(jsonPath("$.code").value("VET"))
                .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                .andExpect(jsonPath("$.permissions[0].code").value("ANIMAL_READ"));
    }

    @Test
    @DisplayName("create con name en blanco responde 400")
    void create_con_name_en_blanco_responde_400() throws Exception {
        mockMvc.perform(post("/roles").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"","code":"VET"}
                """)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("listAll responde 200 con la lista global")
    void list_all_responde_200_con_la_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(dto()));

        mockMvc.perform(get("/roles")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ROLE_ID));
    }

    @Test
    @DisplayName("listByCompany responde 200 con los roles de la empresa autenticada")
    void list_by_company_responde_200() throws Exception {
        when(listByCompanyUseCase.listByCompany(WebMvcSliceConfig.COMPANY_ID))
                .thenReturn(List.of(dto()));

        mockMvc.perform(get("/roles/by-company")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("VET"));
    }

    @Test
    @DisplayName("findById responde 200 con el rol")
    void find_by_id_responde_200_con_el_rol() throws Exception {
        when(findUseCase.findById(ROLE_ID)).thenReturn(dto());

        mockMvc.perform(get("/roles/{id}", ROLE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VET"));
    }

    @Test
    @DisplayName("findById con id inexistente responde 404")
    void find_by_id_con_id_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new RoleNotFoundException(99L));

        mockMvc.perform(get("/roles/{id}", 99L)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("update responde 200 con el rol actualizado")
    void update_responde_200_con_el_rol_actualizado() throws Exception {
        when(updateUseCase.execute(any(UpdateRoleCommand.class))).thenReturn(dto());

        mockMvc.perform(put("/roles/{id}", ROLE_ID).contentType(MediaType.APPLICATION_JSON)
                .content(CREACION_VALIDA)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("delete responde 204")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/roles/{id}", ROLE_ID)).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(ROLE_ID, WebMvcSliceConfig.COMPANY_ID);
    }

    @Test
    @DisplayName("reactivate responde 200 con el rol reactivado")
    void reactivate_responde_200_con_el_rol_reactivado() throws Exception {
        when(reactivateUseCase.execute(ROLE_ID, WebMvcSliceConfig.COMPANY_ID)).thenReturn(dto());

        mockMvc.perform(patch("/roles/{id}/enable", ROLE_ID)).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
