package com.vetsoftware.app.permission.infrastructure.web;

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

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.CompanySummaryDto;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.permission.application.port.in.CreatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.DeletePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.FindPermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsByCompanyUseCase;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsUseCase;
import com.vetsoftware.app.permission.application.port.in.ReactivatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.UpdatePermissionUseCase;
import com.vetsoftware.app.permission.domain.PermissionNotFoundException;
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

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PermissionController — contrato HTTP")
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePermissionUseCase createUseCase;
    @MockitoBean
    private UpdatePermissionUseCase updateUseCase;
    @MockitoBean
    private FindPermissionUseCase findUseCase;
    @MockitoBean
    private ListPermissionsUseCase listUseCase;
    @MockitoBean
    private ListPermissionsByCompanyUseCase listByCompanyUseCase;
    @MockitoBean
    private DeletePermissionUseCase deleteUseCase;
    @MockitoBean
    private ReactivatePermissionUseCase reactivateUseCase;
    @MockitoBean
    private Authz authz;

    private static PermissionDto crearFactura() {
        return new PermissionDto(2L, "Crear factura", "billing.create",
                new CompanySummaryDto(9L, "Clinica Norte", "NIT-900"),
                new SubModuleSummaryDto(5L, "Inventario", "INV"),
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /permissions responde 201 con el recurso creado, usando la empresa del contexto")
    void post_responde_201() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(createUseCase.execute(any())).thenReturn(crearFactura());

        mockMvc.perform(post("/permissions").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Crear factura\",\"code\":\"billing.create\",\"subModuleId\":5}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.company.id").value(9))
                .andExpect(jsonPath("$.subModule.id").value(5));
    }

    @Test
    @DisplayName("POST /permissions traduce el request al command con la empresa del contexto, no del body")
    void post_traduce_el_request_al_command() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(createUseCase.execute(any())).thenReturn(crearFactura());

        mockMvc.perform(post("/permissions").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Crear factura\",\"code\":\"billing.create\",\"subModuleId\":5}"));

        verify(createUseCase)
                .execute(new CreatePermissionCommand("Crear factura", "billing.create", 9L, 5L));
    }

    @Test
    @DisplayName("POST /permissions con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"code\":\"billing.create\",\"subModuleId\":5}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /permissions devuelve la lista global")
    void get_lista_global() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(crearFactura()));

        mockMvc.perform(get("/permissions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /permissions/by-company acota por la empresa del contexto")
    void get_lista_por_empresa() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(listByCompanyUseCase.listByCompany(9L)).thenReturn(List.of(crearFactura()));

        mockMvc.perform(get("/permissions/by-company")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].company.id").value(9));
    }

    @Test
    @DisplayName("GET /permissions/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new PermissionNotFoundException(99L));

        mockMvc.perform(get("/permissions/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /permissions/{id} responde 200 con el recurso encontrado")
    void get_por_id_responde_200() throws Exception {
        when(findUseCase.findById(2L)).thenReturn(crearFactura());

        mockMvc.perform(get("/permissions/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("billing.create"));
    }

    @Test
    @DisplayName("PUT /permissions/{id} responde 200")
    void put_responde_200() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(updateUseCase.execute(any())).thenReturn(crearFactura());

        mockMvc.perform(put("/permissions/2").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Crear factura\",\"code\":\"billing.create\",\"subModuleId\":5}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /permissions/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/permissions/2")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(2L);
    }

    @Test
    @DisplayName("PATCH /permissions/{id}/enable acota por la empresa del contexto y responde 200")
    void patch_enable_responde_200() throws Exception {
        when(authz.currentCompanyIdOrNull()).thenReturn(9L);
        when(reactivateUseCase.execute(2L, 9L)).thenReturn(crearFactura());

        mockMvc.perform(patch("/permissions/2/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(2L, 9L);
    }

    /**
     * Un SYSTEM puro no tiene empresa seleccionada: el controller pasa {@code null}
     * y el caso de uso opera global, sin acotar.
     */
    @Test
    @DisplayName("PATCH /permissions/{id}/enable sin empresa en el contexto pasa null")
    void patch_enable_sin_empresa_pasa_null() throws Exception {
        when(authz.currentCompanyIdOrNull()).thenReturn(null);
        when(reactivateUseCase.execute(2L, null)).thenReturn(crearFactura());

        mockMvc.perform(patch("/permissions/2/enable")).andExpect(status().isOk());

        verify(reactivateUseCase).execute(2L, null);
    }
}
