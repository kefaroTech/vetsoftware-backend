package com.vetsoftware.app.submodule.infrastructure.web;

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

import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.ModuleSummaryDto;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.CreateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.DeleteSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.FindSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.ListSubModulesUseCase;
import com.vetsoftware.app.submodule.application.port.in.ReactivateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.UpdateSubModuleUseCase;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
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
@WebMvcTest(SubModuleController.class)
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SubModuleController — contrato HTTP")
class SubModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSubModuleUseCase createUseCase;
    @MockitoBean
    private UpdateSubModuleUseCase updateUseCase;
    @MockitoBean
    private FindSubModuleUseCase findUseCase;
    @MockitoBean
    private ListSubModulesUseCase listUseCase;
    @MockitoBean
    private DeleteSubModuleUseCase deleteUseCase;
    @MockitoBean
    private ReactivateSubModuleUseCase reactivateUseCase;

    private static SubModuleDto reportes() {
        return new SubModuleDto(2L, "Reportes", "REP",
                new ModuleSummaryDto(1L, "Facturacion", "FACT"), true, false,
                LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /sub-modules responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(reportes());

        mockMvc.perform(post("/sub-modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reportes\",\"code\":\"REP\",\"moduleId\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Reportes"))
                .andExpect(jsonPath("$.code").value("REP"))
                .andExpect(jsonPath("$.module.name").value("Facturacion"))
                .andExpect(jsonPath("$.sellable").value(true))
                .andExpect(jsonPath("$.readOnlyCapable").value(false));
    }

    @Test
    @DisplayName("POST /sub-modules traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(reportes());

        mockMvc.perform(post("/sub-modules").contentType(MediaType.APPLICATION_JSON).content(
                "{\"name\":\"Reportes\",\"code\":\"REP\",\"moduleId\":1,\"sellable\":true,\"readOnlyCapable\":true}"));

        verify(createUseCase)
                .execute(new CreateSubModuleCommand("Reportes", "REP", 1L, true, true));
    }

    /**
     * Los dos defaults de la especificacion son {@code false}: si nadie decide si
     * un submodulo es comercial o si sabe funcionar en solo lectura, el fallo
     * seguro es que no aparezca en el catalogo y que se oculte al darse de baja.
     */
    @Test
    @DisplayName("un cuerpo que omite las dos banderas las manda en falso, no las inventa")
    void un_cuerpo_sin_banderas_las_manda_en_falso() throws Exception {
        when(createUseCase.execute(any())).thenReturn(reportes());

        mockMvc.perform(post("/sub-modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reportes\",\"code\":\"REP\",\"moduleId\":1}"));

        verify(createUseCase)
                .execute(new CreateSubModuleCommand("Reportes", "REP", 1L, false, false));
    }

    @Test
    @DisplayName("POST /sub-modules con nombre vacio responde 400 y no llega al caso de uso")
    void post_con_nombre_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/sub-modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"code\":\"REP\",\"moduleId\":1}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /sub-modules devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(reportes()));

        mockMvc.perform(get("/sub-modules")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /sub-modules/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new SubModuleNotFoundException(99L));

        mockMvc.perform(get("/sub-modules/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /sub-modules/{id} existente responde 200 con el recurso encontrado")
    void get_existente_responde_200() throws Exception {
        when(findUseCase.findById(2L)).thenReturn(reportes());

        mockMvc.perform(get("/sub-modules/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Reportes"))
                .andExpect(jsonPath("$.module.name").value("Facturacion"));
    }

    @Test
    @DisplayName("PUT /sub-modules/{id} responde 200")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(reportes());

        mockMvc.perform(put("/sub-modules/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reportes\",\"code\":\"REP\",\"moduleId\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /sub-modules/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/sub-modules/2")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(2L);
    }

    @Test
    @DisplayName("PATCH /sub-modules/{id}/enable responde 200 con el submodulo reactivado")
    void patch_enable_responde_200_con_el_submodulo_reactivado() throws Exception {
        when(reactivateUseCase.execute(2L)).thenReturn(reportes());

        mockMvc.perform(patch("/sub-modules/2/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(2L);
    }
}
