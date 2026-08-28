package com.vetsoftware.app.catalogitem.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.in.CreateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.DeleteCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.FindCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ListCatalogItemsUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.ReactivateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.application.port.in.UpdateCatalogItemUseCase;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.shared.pagination.PageResult;
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

/**
 * Rodaja HTTP del controller: rutas, binding, validación del request, códigos
 * de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * <strong>Lo que aquí todavía no se puede afirmar.</strong> Las excepciones de
 * dominio de esta feature aún no están cableadas en
 * {@code GlobalExceptionHandler} —lo lleva otro agente—, así que un
 * {@code CatalogItemNotFoundException} saldría hoy como 500 y un test que
 * esperara 404 estaría comprobando una mentira. Las aserciones de 404 y 409 se
 * añaden en cuanto el handler tenga sus entradas; el 400 de validación sí se
 * cubre, porque ese camino es genérico y ya existe.
 */
@WebMvcTest(CatalogItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CatalogItemController — contrato HTTP")
class CatalogItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCatalogItemUseCase createUseCase;
    @MockitoBean
    private UpdateCatalogItemUseCase updateUseCase;
    @MockitoBean
    private FindCatalogItemUseCase findUseCase;
    @MockitoBean
    private ListCatalogItemsUseCase listUseCase;
    @MockitoBean
    private DeleteCatalogItemUseCase deleteUseCase;
    @MockitoBean
    private ReactivateCatalogItemUseCase reactivateUseCase;

    private static CatalogItemDto usuarioExtra() {
        return new CatalogItemDto(2L, "EXTRA_USER", "Usuario adicional", "Un usuario más",
                "Detalle largo", ItemType.CAPACITY, "USER", false, 1, 50, 7,
                CatalogItemStatus.ACTIVE, LocalDateTime.of(2026, 8, 22, 10, 15, 30), true);
    }

    private static final String CUERPO_VALIDO = """
            {"code":"EXTRA_USER","name":"Usuario adicional","shortDescription":"Un usuario más",
             "longDescription":"Detalle largo","itemType":"CAPACITY","capacityUnit":"USER",
             "core":false,"minQuantity":1,"maxQuantity":50,"sortOrder":7,"status":"ACTIVE"}
            """;

    @Test
    @DisplayName("POST /catalog-items responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(usuarioExtra());

        mockMvc.perform(post("/catalog-items").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("EXTRA_USER"))
                .andExpect(jsonPath("$.itemType").value("CAPACITY"))
                .andExpect(jsonPath("$.capacityUnit").value("USER"))
                .andExpect(jsonPath("$.maxQuantity").value(50))
                .andExpect(jsonPath("$.sortOrder").value(7))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(usuarioExtra());

        mockMvc.perform(post("/catalog-items").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_VALIDO));

        ArgumentCaptor<CreateCatalogItemCommand> command = ArgumentCaptor
                .forClass(CreateCatalogItemCommand.class);
        verify(createUseCase).execute(command.capture());
        assertThat(command.getValue().code()).isEqualTo("EXTRA_USER");
        assertThat(command.getValue().itemType()).isEqualTo(ItemType.CAPACITY);
        assertThat(command.getValue().capacityUnit()).isEqualTo("USER");
        assertThat(command.getValue().minQuantity()).isEqualTo(1);
        assertThat(command.getValue().maxQuantity()).isEqualTo(50);
        assertThat(command.getValue().sortOrder()).isEqualTo(7);
    }

    /**
     * Los defaults de la ficha 1 se aplican en la frontera cuando el cuerpo omite
     * el campo. Distinguir «no lo mandó» de «mandó cero» es lo que hace que
     * {@code sortOrder} nulo no acabe siendo un cero involuntario en un artículo
     * que sí quería el 0 explícito.
     */
    @Test
    @DisplayName("POST completa minQuantity, sortOrder y status con los defaults de la ficha")
    void post_completa_los_defaults() throws Exception {
        when(createUseCase.execute(any())).thenReturn(usuarioExtra());

        mockMvc.perform(post("/catalog-items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"CORE\",\"name\":\"Núcleo\",\"itemType\":\"MODULE\"}"));

        ArgumentCaptor<CreateCatalogItemCommand> command = ArgumentCaptor
                .forClass(CreateCatalogItemCommand.class);
        verify(createUseCase).execute(command.capture());
        assertThat(command.getValue().minQuantity()).isEqualTo(1);
        assertThat(command.getValue().sortOrder()).isZero();
        assertThat(command.getValue().status()).isNull();
    }

    @Test
    @DisplayName("POST sin código responde 400 y no llega al caso de uso")
    void post_sin_codigo_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sin código\",\"itemType\":\"MODULE\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST con cantidad mínima negativa responde 400: la restricción sí se evalúa")
    void post_con_cantidad_negativa_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-items").contentType(MediaType.APPLICATION_JSON).content(
                "{\"code\":\"C\",\"name\":\"N\",\"itemType\":\"MODULE\",\"minQuantity\":-1}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST con un tipo de artículo que no existe responde 400")
    void post_con_tipo_desconocido_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"C\",\"name\":\"N\",\"itemType\":\"INVENTADO\"}"))
                .andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /catalog-items devuelve la página con sus cinco campos")
    void get_listado_devuelve_la_pagina() throws Exception {
        when(listUseCase.listAll(0, 20))
                .thenReturn(new PageResult<>(List.of(usuarioExtra()), 0, 20, 1L, 1));

        mockMvc.perform(get("/catalog-items")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("EXTRA_USER"))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /catalog-items pasa page y pageSize tal como llegan")
    void get_listado_pasa_los_parametros() throws Exception {
        when(listUseCase.listAll(3, 50)).thenReturn(PageResult.empty(3, 50));

        mockMvc.perform(get("/catalog-items").param("page", "3").param("pageSize", "50"))
                .andExpect(status().isOk());

        verify(listUseCase).listAll(3, 50);
    }

    @Test
    @DisplayName("GET /catalog-items/{id} devuelve el artículo")
    void get_por_id() throws Exception {
        when(findUseCase.findById(2L)).thenReturn(usuarioExtra());

        mockMvc.perform(get("/catalog-items/2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Usuario adicional"));
    }

    @Test
    @DisplayName("PUT /catalog-items/{id} toma el id de la ruta, nunca del cuerpo")
    void put_toma_el_id_de_la_ruta() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(usuarioExtra());

        mockMvc.perform(put("/catalog-items/2").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_VALIDO)).andExpect(status().isOk());

        ArgumentCaptor<UpdateCatalogItemCommand> command = ArgumentCaptor
                .forClass(UpdateCatalogItemCommand.class);
        verify(updateUseCase).execute(command.capture());
        assertThat(command.getValue().id()).isEqualTo(2L);
        assertThat(command.getValue().name()).isEqualTo("Usuario adicional");
    }

    @Test
    @DisplayName("PUT sin estado responde 400: es obligatorio al editar")
    void put_sin_estado_responde_400() throws Exception {
        mockMvc.perform(put("/catalog-items/2").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"N\",\"itemType\":\"MODULE\"}"))
                .andExpect(status().isBadRequest());

        verify(updateUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("DELETE /catalog-items/{id} responde 204 sin cuerpo")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/catalog-items/2")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(eq(2L));
    }

    @Test
    @DisplayName("PATCH /catalog-items/{id}/enable reactiva y devuelve el artículo")
    void patch_enable_reactiva() throws Exception {
        when(reactivateUseCase.execute(2L)).thenReturn(usuarioExtra());

        mockMvc.perform(patch("/catalog-items/2/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
