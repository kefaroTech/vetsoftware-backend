package com.vetsoftware.app.basepermission.infrastructure.web;

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

import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.basepermission.application.port.in.CreateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.DeleteBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.FindBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.ListBasePermissionsUseCase;
import com.vetsoftware.app.basepermission.application.port.in.ReactivateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.UpdateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.domain.BasePermissionNotFoundException;
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

/**
 * Rodaja HTTP del controller: rutas, binding, validacion del request, codigos
 * de estado y forma del JSON. Lo que hay debajo son dobles — aqui no se prueba
 * el caso de uso, se prueba el contrato que ve el front.
 */
@WebMvcTest(BasePermissionController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BasePermissionController — contrato HTTP")
class BasePermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBasePermissionUseCase createUseCase;
    @MockitoBean
    private UpdateBasePermissionUseCase updateUseCase;
    @MockitoBean
    private FindBasePermissionUseCase findUseCase;
    @MockitoBean
    private ListBasePermissionsUseCase listUseCase;
    @MockitoBean
    private DeleteBasePermissionUseCase deleteUseCase;
    @MockitoBean
    private ReactivateBasePermissionUseCase reactivateUseCase;

    private static BasePermissionDto crearFactura() {
        return new BasePermissionDto(2L, "Crear factura", "INVOICE_CREATE",
                new SubModuleSummaryDto(1L, "Ventas", "VEN"), LocalDateTime.of(2026, 1, 15, 10, 30),
                true);
    }

    @Test
    @DisplayName("POST /base-permissions responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(crearFactura());

        mockMvc.perform(post("/base-permissions").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Crear factura\",\"code\":\"INVOICE_CREATE\",\"subModuleId\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Crear factura"))
                .andExpect(jsonPath("$.code").value("INVOICE_CREATE"))
                .andExpect(jsonPath("$.subModule.name").value("Ventas"));
    }

    @Test
    @DisplayName("POST /base-permissions traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(crearFactura());

        mockMvc.perform(post("/base-permissions").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Crear factura\",\"code\":\"INVOICE_CREATE\",\"subModuleId\":1}"));

        verify(createUseCase)
                .execute(new CreateBasePermissionCommand("Crear factura", "INVOICE_CREATE", 1L));
    }

    @Test
    @DisplayName("POST /base-permissions con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/base-permissions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"code\":\"INVOICE_CREATE\",\"subModuleId\":1}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /base-permissions devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(crearFactura()));

        mockMvc.perform(get("/base-permissions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /base-permissions/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new BasePermissionNotFoundException(99L));

        mockMvc.perform(get("/base-permissions/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /base-permissions/{id} responde 200")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(crearFactura());

        mockMvc.perform(put("/base-permissions/2").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Crear factura\",\"code\":\"INVOICE_CREATE\",\"subModuleId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /base-permissions/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/base-permissions/2")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(2L);
    }

    @Test
    @DisplayName("PATCH /base-permissions/{id}/enable responde 200 con el permiso reactivado")
    void patch_enable_responde_200_con_el_permiso_reactivado() throws Exception {
        when(reactivateUseCase.execute(2L)).thenReturn(crearFactura());

        mockMvc.perform(patch("/base-permissions/2/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(2L);
    }
}
