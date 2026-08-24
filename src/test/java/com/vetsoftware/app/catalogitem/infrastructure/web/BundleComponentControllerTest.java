package com.vetsoftware.app.catalogitem.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.vetsoftware.app.catalogitem.application.command.CreateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.command.UpdateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteBundleComponentUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListBundleComponentsUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateBundleComponentUseCase;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BundleComponentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("BundleComponentController — contrato HTTP")
class BundleComponentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBundleComponentUseCase createUseCase;
    @MockitoBean
    private UpdateBundleComponentUseCase updateUseCase;
    @MockitoBean
    private ListBundleComponentsUseCase listUseCase;
    @MockitoBean
    private DeleteBundleComponentUseCase deleteUseCase;

    private static BundleComponentDto componente() {
        return new BundleComponentDto(70L, 3L, 2L, 5, LocalDateTime.of(2026, 8, 22, 10, 15, 30),
                true);
    }

    @Test
    @DisplayName("POST responde 201 con la pieza añadida")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(componente());

        mockMvc.perform(post("/catalog-items/3/components").contentType(MediaType.APPLICATION_JSON)
                .content("{\"componentItemId\":2,\"quantity\":5}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(70))
                .andExpect(jsonPath("$.bundleItemId").value(3))
                .andExpect(jsonPath("$.componentItemId").value(2))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    @DisplayName("POST toma el paquete de la ruta y la pieza del cuerpo")
    void post_no_cruza_paquete_y_pieza() throws Exception {
        when(createUseCase.execute(any())).thenReturn(componente());

        mockMvc.perform(post("/catalog-items/3/components").contentType(MediaType.APPLICATION_JSON)
                .content("{\"componentItemId\":2,\"quantity\":5}"));

        ArgumentCaptor<CreateBundleComponentCommand> command = ArgumentCaptor
                .forClass(CreateBundleComponentCommand.class);
        verify(createUseCase).execute(command.capture());
        assertThat(command.getValue().bundleItemId()).isEqualTo(3L);
        assertThat(command.getValue().componentItemId()).isEqualTo(2L);
        assertThat(command.getValue().quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("POST con cantidad cero responde 400: la restricción sí se evalúa")
    void post_con_cantidad_cero_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-items/3/components").contentType(MediaType.APPLICATION_JSON)
                .content("{\"componentItemId\":2,\"quantity\":0}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST sin la pieza responde 400 y no llega al caso de uso")
    void post_sin_pieza_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-items/3/components").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":5}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET lista las piezas del paquete")
    void get_lista_las_piezas() throws Exception {
        when(listUseCase.listByBundle(3L)).thenReturn(List.of(componente()));

        mockMvc.perform(get("/catalog-items/3/components")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(5));
    }

    @Test
    @DisplayName("PUT lleva al command el id de la pieza y el del paquete de la ruta")
    void put_lleva_los_dos_identificadores() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(componente());

        mockMvc.perform(put("/catalog-items/3/components/70")
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":9}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateBundleComponentCommand> command = ArgumentCaptor
                .forClass(UpdateBundleComponentCommand.class);
        verify(updateUseCase).execute(command.capture());
        assertThat(command.getValue().id()).isEqualTo(70L);
        assertThat(command.getValue().bundleItemId()).isEqualTo(3L);
        assertThat(command.getValue().quantity()).isEqualTo(9);
    }

    @Test
    @DisplayName("DELETE responde 204 y pasa los dos identificadores de la ruta")
    void delete_pasa_los_dos_identificadores() throws Exception {
        mockMvc.perform(delete("/catalog-items/3/components/70")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(3L, 70L);
    }
}
