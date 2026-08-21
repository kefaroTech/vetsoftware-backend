package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.CreateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.DeleteDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.FindDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListAvailableDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.UpdateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingTypeNotFoundException;
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
 * de estado y forma del JSON. Lo que hay debajo son dobles.
 */
@WebMvcTest(DiagnosticImagingTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("DiagnosticImagingTypeController — contrato HTTP")
class DiagnosticImagingTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDiagnosticImagingTypeUseCase createUseCase;
    @MockitoBean
    private UpdateDiagnosticImagingTypeUseCase updateUseCase;
    @MockitoBean
    private FindDiagnosticImagingTypeUseCase findUseCase;
    @MockitoBean
    private ListDiagnosticImagingTypesUseCase listUseCase;
    @MockitoBean
    private ListAvailableDiagnosticImagingTypesUseCase listAvailableUseCase;
    @MockitoBean
    private DeleteDiagnosticImagingTypeUseCase deleteUseCase;
    @MockitoBean
    private Authz authz;

    private static DiagnosticImagingTypeDto tipoDeEmpresa() {
        return new DiagnosticImagingTypeDto(501L, "Ecografia abdominal", "Ecografia de rutina",
                new CompanySummaryDto(9L, "Clinica Norte", "900123456"), false,
                LocalDateTime.of(2026, 1, 15, 10, 0), true);
    }

    private static DiagnosticImagingTypeDto tipoGeneral() {
        return new DiagnosticImagingTypeDto(502L, "Radiografia", "Radiografia simple", null, true,
                LocalDateTime.of(2026, 1, 15, 10, 0), true);
    }

    @Test
    @DisplayName("POST /diagnostic-imaging-types responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(createUseCase.execute(any())).thenReturn(tipoDeEmpresa());

        mockMvc.perform(
                post("/diagnostic-imaging-types").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"name\":\"Ecografia abdominal\",\"description\":\"Ecografia de rutina\",\"general\":false}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.company.id").value(9));
    }

    @Test
    @DisplayName("POST /diagnostic-imaging-types traduce el request al command con la empresa del contexto, no del body")
    void post_traduce_el_request_al_command_con_la_empresa_del_contexto() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(createUseCase.execute(any())).thenReturn(tipoDeEmpresa());

        mockMvc.perform(
                post("/diagnostic-imaging-types").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"name\":\"Ecografia abdominal\",\"description\":\"Ecografia de rutina\",\"general\":false}"));

        verify(createUseCase).execute(new CreateDiagnosticImagingTypeCommand("Ecografia abdominal",
                "Ecografia de rutina", 9L, false));
    }

    @Test
    @DisplayName("POST /diagnostic-imaging-types con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/diagnostic-imaging-types").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"description\":\"desc\",\"general\":false}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /diagnostic-imaging-types devuelve el listado global")
    void get_lista_global() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(tipoGeneral(), tipoDeEmpresa()));

        mockMvc.perform(get("/diagnostic-imaging-types")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(502))
                .andExpect(jsonPath("$[1].id").value(501));
    }

    @Test
    @DisplayName("GET /diagnostic-imaging-types/available acota por la empresa del contexto")
    void get_disponibles_acota_por_la_empresa() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(listAvailableUseCase.listAvailable(9L)).thenReturn(List.of(tipoDeEmpresa()));

        mockMvc.perform(get("/diagnostic-imaging-types/available")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].company.id").value(9));
    }

    @Test
    @DisplayName("GET /diagnostic-imaging-types/{id} inexistente responde 404, no 500")
    void get_por_id_inexistente_responde_404() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(findUseCase.findById(999L, 9L))
                .thenThrow(new DiagnosticImagingTypeNotFoundException(999L));

        mockMvc.perform(get("/diagnostic-imaging-types/999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /diagnostic-imaging-types/{id} responde 200 con el recurso encontrado")
    void get_por_id_responde_200() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(findUseCase.findById(501L, 9L)).thenReturn(tipoDeEmpresa());

        mockMvc.perform(get("/diagnostic-imaging-types/501")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(501));
    }

    @Test
    @DisplayName("PUT /diagnostic-imaging-types/{id} responde 200")
    void put_responde_200() throws Exception {
        when(authz.currentCompanyId()).thenReturn(9L);
        when(updateUseCase.execute(any())).thenReturn(tipoDeEmpresa());

        mockMvc.perform(put("/diagnostic-imaging-types/501").contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"name\":\"Ecografia abdominal\",\"description\":\"Ecografia de rutina\",\"general\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /diagnostic-imaging-types/{id} responde 204 y propaga la empresa del contexto")
    void delete_responde_204() throws Exception {
        when(authz.currentCompanyIdOrNull()).thenReturn(9L);

        mockMvc.perform(delete("/diagnostic-imaging-types/501")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(501L, 9L);
    }
}
