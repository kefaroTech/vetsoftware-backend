package com.vetsoftware.app.module.infrastructure.web;

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

import com.vetsoftware.app.module.application.command.CreateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.in.CreateModuleUseCase;
import com.vetsoftware.app.module.application.port.in.DeleteModuleUseCase;
import com.vetsoftware.app.module.application.port.in.FindModuleUseCase;
import com.vetsoftware.app.module.application.port.in.ListModulesUseCase;
import com.vetsoftware.app.module.application.port.in.ReactivateModuleUseCase;
import com.vetsoftware.app.module.application.port.in.UpdateModuleUseCase;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
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

@WebMvcTest(ModuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ModuleController — contrato HTTP")
class ModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateModuleUseCase createUseCase;
    @MockitoBean
    private UpdateModuleUseCase updateUseCase;
    @MockitoBean
    private FindModuleUseCase findUseCase;
    @MockitoBean
    private ListModulesUseCase listUseCase;
    @MockitoBean
    private DeleteModuleUseCase deleteUseCase;
    @MockitoBean
    private ReactivateModuleUseCase reactivateUseCase;

    private static ModuleDto inventario() {
        return new ModuleDto(1L, "Inventario", "INV", LocalDateTime.of(2026, 1, 15, 10, 30), true);
    }

    @Test
    @DisplayName("POST /modules responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(inventario());

        mockMvc.perform(post("/modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Inventario\",\"code\":\"INV\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Inventario"))
                .andExpect(jsonPath("$.code").value("INV"));
    }

    @Test
    @DisplayName("POST /modules traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(inventario());

        mockMvc.perform(post("/modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Inventario\",\"code\":\"INV\"}"));

        verify(createUseCase).execute(new CreateModuleCommand("Inventario", "INV"));
    }

    @Test
    @DisplayName("POST /modules con name vacio responde 400 y no llega al caso de uso")
    void post_con_name_vacio_responde_400() throws Exception {
        mockMvc.perform(post("/modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"code\":\"INV\"}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /modules devuelve la lista")
    void get_lista() throws Exception {
        when(listUseCase.listAll()).thenReturn(List.of(inventario()));

        mockMvc.perform(get("/modules")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /modules/{id} inexistente responde 404, no 500")
    void get_inexistente_responde_404() throws Exception {
        when(findUseCase.findById(99L)).thenThrow(new ModuleNotFoundException(99L));

        mockMvc.perform(get("/modules/99")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /modules/{id} responde 200 con el recurso encontrado")
    void get_por_id_responde_200() throws Exception {
        when(findUseCase.findById(1L)).thenReturn(inventario());

        mockMvc.perform(get("/modules/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /modules/{id} responde 200")
    void put_responde_200() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(inventario());

        mockMvc.perform(put("/modules/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Inventario\",\"code\":\"INV\"}")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /modules/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/modules/1")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(1L);
    }

    @Test
    @DisplayName("PATCH /modules/{id}/enable responde 200 con el modulo reactivado")
    void patch_enable_responde_200() throws Exception {
        when(reactivateUseCase.execute(1L)).thenReturn(inventario());

        mockMvc.perform(patch("/modules/1/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.enabled").value(true));

        verify(reactivateUseCase).execute(1L);
    }
}
