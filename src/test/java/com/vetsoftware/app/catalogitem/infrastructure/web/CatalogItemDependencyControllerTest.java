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

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemDependenciesUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateCatalogItemDependencyUseCase;
import com.vetsoftware.app.catalogitem.domain.RelationType;
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

@WebMvcTest(CatalogItemDependencyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CatalogItemDependencyController — contrato HTTP")
class CatalogItemDependencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCatalogItemDependencyUseCase createUseCase;
    @MockitoBean
    private UpdateCatalogItemDependencyUseCase updateUseCase;
    @MockitoBean
    private ListCatalogItemDependenciesUseCase listUseCase;
    @MockitoBean
    private DeleteCatalogItemDependencyUseCase deleteUseCase;

    private static CatalogItemDependencyDto dependencia() {
        return new CatalogItemDependencyDto(9L, 1L, 2L, RelationType.REQUIRES, "Necesitas caja",
                LocalDateTime.of(2026, 8, 22, 10, 15, 30), true);
    }

    /**
     * El choque C2 de la especificación viaja por el cable: los códigos son
     * {@code REQUIRES}/{@code RECOMMENDS}/{@code EXCLUDES} en inglés, no la terna
     * en español del documento de diseño. Si alguien los cambiara, este test lo
     * dice antes que el front.
     */
    @Test
    @DisplayName("POST responde 201 y el tipo de relación viaja en inglés")
    void post_responde_201_con_el_tipo_en_ingles() throws Exception {
        when(createUseCase.execute(any())).thenReturn(dependencia());

        mockMvc.perform(post("/catalog-items/1/dependencies")
                .contentType(MediaType.APPLICATION_JSON).content(
                        "{\"relatedItemId\":2,\"relationType\":\"REQUIRES\",\"note\":\"Necesitas caja\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationType").value("REQUIRES"))
                .andExpect(jsonPath("$.catalogItemId").value(1))
                .andExpect(jsonPath("$.relatedItemId").value(2))
                .andExpect(jsonPath("$.note").value("Necesitas caja"));
    }

    @Test
    @DisplayName("POST toma el artículo sujeto de la ruta y el relacionado del cuerpo")
    void post_no_cruza_los_dos_articulos() throws Exception {
        when(createUseCase.execute(any())).thenReturn(dependencia());

        mockMvc.perform(
                post("/catalog-items/1/dependencies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relatedItemId\":2,\"relationType\":\"EXCLUDES\"}"));

        ArgumentCaptor<CreateCatalogItemDependencyCommand> command = ArgumentCaptor
                .forClass(CreateCatalogItemDependencyCommand.class);
        verify(createUseCase).execute(command.capture());
        assertThat(command.getValue().catalogItemId()).isEqualTo(1L);
        assertThat(command.getValue().relatedItemId()).isEqualTo(2L);
        assertThat(command.getValue().relationType()).isEqualTo(RelationType.EXCLUDES);
    }

    @Test
    @DisplayName("POST sin tipo de relación responde 400 y no llega al caso de uso")
    void post_sin_tipo_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-items/1/dependencies")
                .contentType(MediaType.APPLICATION_JSON).content("{\"relatedItemId\":2}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST con un tipo de relación que no existe responde 400")
    void post_con_tipo_desconocido_responde_400() throws Exception {
        mockMvc.perform(
                post("/catalog-items/1/dependencies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relatedItemId\":2,\"relationType\":\"REQUIERE\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST con una nota de más de 255 caracteres responde 400")
    void post_con_nota_larga_responde_400() throws Exception {
        mockMvc.perform(
                post("/catalog-items/1/dependencies").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relatedItemId\":2,\"relationType\":\"REQUIRES\",\"note\":\""
                                + "N".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET lista las reglas del artículo")
    void get_lista_las_reglas() throws Exception {
        when(listUseCase.listByCatalogItem(1L)).thenReturn(List.of(dependencia()));

        mockMvc.perform(get("/catalog-items/1/dependencies")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].relationType").value("REQUIRES"));
    }

    @Test
    @DisplayName("PUT lleva al command el id de la regla y el del artículo de la ruta")
    void put_lleva_los_dos_identificadores() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(dependencia());

        mockMvc.perform(
                put("/catalog-items/1/dependencies/9").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relationType\":\"RECOMMENDS\",\"note\":\"Mejor con caja\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateCatalogItemDependencyCommand> command = ArgumentCaptor
                .forClass(UpdateCatalogItemDependencyCommand.class);
        verify(updateUseCase).execute(command.capture());
        assertThat(command.getValue().id()).isEqualTo(9L);
        assertThat(command.getValue().catalogItemId()).isEqualTo(1L);
        assertThat(command.getValue().relationType()).isEqualTo(RelationType.RECOMMENDS);
    }

    @Test
    @DisplayName("DELETE responde 204 y pasa los dos identificadores de la ruta")
    void delete_pasa_los_dos_identificadores() throws Exception {
        mockMvc.perform(delete("/catalog-items/1/dependencies/9"))
                .andExpect(status().isNoContent());

        verify(deleteUseCase).execute(1L, 9L);
    }
}
