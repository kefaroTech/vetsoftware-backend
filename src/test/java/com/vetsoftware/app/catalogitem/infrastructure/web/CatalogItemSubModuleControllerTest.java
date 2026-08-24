package com.vetsoftware.app.catalogitem.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemSubModuleCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import com.vetsoftware.app.catalogitem.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemSubModuleUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemSubModuleUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemSubModulesUseCase;
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

@WebMvcTest(CatalogItemSubModuleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CatalogItemSubModuleController — contrato HTTP")
class CatalogItemSubModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCatalogItemSubModuleUseCase createUseCase;
    @MockitoBean
    private ListCatalogItemSubModulesUseCase listUseCase;
    @MockitoBean
    private DeleteCatalogItemSubModuleUseCase deleteUseCase;

    private static CatalogItemSubModuleDto vinculo() {
        return new CatalogItemSubModuleDto(5L, 1L,
                new SubModuleSummaryDto(50L, "Consultas", "CONSULTATIONS"),
                LocalDateTime.of(2026, 8, 22, 10, 15, 30), true);
    }

    @Test
    @DisplayName("POST responde 201 con el vínculo y su submódulo")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(vinculo());

        mockMvc.perform(post("/catalog-items/1/sub-modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subModuleId\":50}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.catalogItemId").value(1))
                .andExpect(jsonPath("$.subModule.code").value("CONSULTATIONS"));
    }

    /**
     * El artículo viene de la ruta y el submódulo del cuerpo. Cruzarlos daría un
     * vínculo colgado del artículo equivocado sin que ningún test de servicio lo
     * viera.
     */
    @Test
    @DisplayName("POST toma el artículo de la ruta y el submódulo del cuerpo")
    void post_toma_el_articulo_de_la_ruta() throws Exception {
        when(createUseCase.execute(any())).thenReturn(vinculo());

        mockMvc.perform(post("/catalog-items/1/sub-modules").contentType(MediaType.APPLICATION_JSON)
                .content("{\"subModuleId\":50}"));

        ArgumentCaptor<CreateCatalogItemSubModuleCommand> command = ArgumentCaptor
                .forClass(CreateCatalogItemSubModuleCommand.class);
        verify(createUseCase).execute(command.capture());
        assertThat(command.getValue().catalogItemId()).isEqualTo(1L);
        assertThat(command.getValue().subModuleId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("POST sin submódulo responde 400 y no llega al caso de uso")
    void post_sin_submodulo_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-items/1/sub-modules").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET lista los vínculos del artículo de la ruta")
    void get_lista_los_vinculos() throws Exception {
        when(listUseCase.listByCatalogItem(1L)).thenReturn(List.of(vinculo()));

        mockMvc.perform(get("/catalog-items/1/sub-modules")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subModule.name").value("Consultas"))
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    @DisplayName("DELETE responde 204 y pasa los dos identificadores de la ruta")
    void delete_pasa_los_dos_identificadores() throws Exception {
        mockMvc.perform(delete("/catalog-items/1/sub-modules/5")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(1L, 5L);
    }
}
