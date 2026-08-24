package com.vetsoftware.app.pricelist.infrastructure.web;

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
import com.vetsoftware.app.pricelist.application.command.CreatePriceListCommand;
import com.vetsoftware.app.pricelist.application.command.PublishPriceListCommand;
import com.vetsoftware.app.pricelist.application.command.UpdatePriceListCommand;
import com.vetsoftware.app.pricelist.application.dto.PriceListDto;
import com.vetsoftware.app.pricelist.application.port.in.ArchivePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.CreatePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.DeletePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.FindPriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.ListPriceListsUseCase;
import com.vetsoftware.app.pricelist.application.port.in.PublishPriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.ReactivatePriceListUseCase;
import com.vetsoftware.app.pricelist.application.port.in.UpdatePriceListUseCase;
import com.vetsoftware.app.pricelist.domain.PriceListStatus;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PriceListController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("PriceListController — contrato HTTP")
class PriceListControllerTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 2, 1);
    private static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final Long FIRMANTE = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePriceListUseCase createUseCase;
    @MockitoBean
    private UpdatePriceListUseCase updateUseCase;
    @MockitoBean
    private FindPriceListUseCase findUseCase;
    @MockitoBean
    private ListPriceListsUseCase listUseCase;
    @MockitoBean
    private DeletePriceListUseCase deleteUseCase;
    @MockitoBean
    private PublishPriceListUseCase publishUseCase;
    @MockitoBean
    private ArchivePriceListUseCase archiveUseCase;
    @MockitoBean
    private ReactivatePriceListUseCase reactivateUseCase;

    /**
     * El doble compartido de {@code WebMvcSliceConfig}. Se re-stubea porque su
     * {@code currentSystemUserId()} no viene stubeado de fabrica y Mockito
     * devolveria 0L para un {@code Long} -no null-: la lista quedaria firmada por
     * un usuario de sistema que no existe y la asercion no lo notaria.
     */
    @Autowired
    private Authz authz;

    @BeforeEach
    void stubDelPrincipal() {
        when(authz.currentSystemUserId()).thenReturn(FIRMANTE);
    }

    private static PriceListDto borrador() {
        return new PriceListDto(1L, "LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null,
                PriceListStatus.DRAFT, null, null, CREADA_EL, true);
    }

    private static PriceListDto publicada() {
        return new PriceListDto(1L, "LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null,
                PriceListStatus.PUBLISHED, LocalDateTime.of(2026, 1, 20, 9, 0), 7L, CREADA_EL,
                true);
    }

    private static final String CUERPO_VALIDO = """
            {"code":"LISTA-2026-01","name":"Tarifa 2026","currency":"COP","validFrom":"2026-02-01"}
            """;

    @Test
    @DisplayName("POST /price-lists responde 201 con el recurso creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(borrador());

        mockMvc.perform(
                post("/price-lists").contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("LISTA-2026-01"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.publishedAt").doesNotExist());
    }

    @Test
    @DisplayName("POST /price-lists traduce el request al command sin inventarse campos")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(borrador());

        mockMvc.perform(post("/price-lists").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_VALIDO));

        verify(createUseCase).execute(
                new CreatePriceListCommand("LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null));
    }

    @Test
    @DisplayName("POST /price-lists no acepta companyId: la tarifa es global de plataforma")
    void el_cuerpo_no_lleva_company_id() throws Exception {
        when(createUseCase.execute(any())).thenReturn(borrador());

        mockMvc.perform(post("/price-lists").contentType(MediaType.APPLICATION_JSON).content(
                """
                        {"code":"LISTA-2026-01","name":"Tarifa 2026","currency":"COP","validFrom":"2026-02-01","companyId":9}
                        """));

        verify(createUseCase).execute(
                new CreatePriceListCommand("LISTA-2026-01", "Tarifa 2026", "COP", DESDE, null));
    }

    @Test
    @DisplayName("POST /price-lists con moneda en minúsculas responde 400 y no llega al caso de uso")
    void post_con_moneda_invalida_responde_400() throws Exception {
        mockMvc.perform(post("/price-lists").contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"L","name":"Tarifa","currency":"cop","validFrom":"2026-02-01"}
                """)).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /price-lists sin validFrom responde 400")
    void post_sin_valid_from_responde_400() throws Exception {
        mockMvc.perform(post("/price-lists").contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"L","name":"Tarifa","currency":"COP"}
                """)).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /price-lists devuelve el contrato único de paginación")
    void get_devuelve_el_contrato_de_paginacion() throws Exception {
        when(listUseCase.listByStatus(null, 0, 20))
                .thenReturn(new PageResult<>(List.of(borrador()), 0, 20, 1L, 1));

        mockMvc.perform(get("/price-lists")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("LISTA-2026-01"))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /price-lists pasa page y pageSize tal como llegan")
    void get_pasa_los_parametros_de_pagina() throws Exception {
        when(listUseCase.listByStatus(null, 3, 50))
                .thenReturn(new PageResult<>(List.of(), 3, 50, 0L, 0));

        mockMvc.perform(get("/price-lists").param("page", "3").param("pageSize", "50"))
                .andExpect(status().isOk());

        verify(listUseCase).listByStatus(null, 3, 50);
    }

    @Test
    @DisplayName("GET /price-lists/{id} devuelve la lista con su firma")
    void get_por_id() throws Exception {
        when(findUseCase.findById(1L)).thenReturn(publicada());

        mockMvc.perform(get("/price-lists/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedBySystemUserId").value(7));
    }

    @Test
    @DisplayName("PUT /price-lists/{id} toma el id de la URL, no del cuerpo")
    void put_toma_el_id_de_la_url() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(borrador());

        mockMvc.perform(put("/price-lists/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Tarifa 2026","currency":"COP","validFrom":"2026-02-01"}
                """)).andExpect(status().isOk());

        verify(updateUseCase)
                .execute(new UpdatePriceListCommand(1L, "Tarifa 2026", "COP", DESDE, null));
    }

    @Test
    @DisplayName("DELETE /price-lists/{id} responde 204")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/price-lists/1")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(1L);
    }

    @Test
    @DisplayName("PATCH /price-lists/{id}/publish congela la tarifa sin recibir cuerpo")
    void patch_publish() throws Exception {
        when(publishUseCase.execute(any())).thenReturn(publicada());

        mockMvc.perform(patch("/price-lists/1/publish")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedBySystemUserId").value(7));
    }

    @Test
    @DisplayName("PATCH /price-lists/{id}/publish firma con el principal, no con el cuerpo")
    void patch_publish_firma_con_el_principal() throws Exception {
        when(publishUseCase.execute(any())).thenReturn(publicada());

        mockMvc.perform(patch("/price-lists/1/publish"));

        verify(publishUseCase).execute(new PublishPriceListCommand(1L, FIRMANTE));
    }

    /**
     * La proteccion que ninguna otra capa cubre: la firma NO viaja en el cuerpo.
     * Aunque el cliente mande un {@code publishedBySystemUserId} ajeno, el endpoint
     * no lo lee y el command sigue llevando el del principal. Es el mismo
     * antipatron que {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} prohibe para
     * {@code companyId}, y por el mismo motivo.
     */
    @Test
    @DisplayName("PATCH /price-lists/{id}/publish ignora una firma ajena metida en el cuerpo")
    void patch_publish_ignora_la_firma_del_cuerpo() throws Exception {
        when(publishUseCase.execute(any())).thenReturn(publicada());

        mockMvc.perform(patch("/price-lists/1/publish").contentType(MediaType.APPLICATION_JSON)
                .content("{\"publishedBySystemUserId\":99}")).andExpect(status().isOk());

        verify(publishUseCase).execute(new PublishPriceListCommand(1L, FIRMANTE));
        verify(publishUseCase, never()).execute(new PublishPriceListCommand(1L, 99L));
    }

    @Test
    @DisplayName("PATCH /price-lists/{id}/archive retira la tarifa")
    void patch_archive() throws Exception {
        when(archiveUseCase.execute(1L)).thenReturn(publicada());

        mockMvc.perform(patch("/price-lists/1/archive")).andExpect(status().isOk());

        verify(archiveUseCase).execute(1L);
    }

    @Test
    @DisplayName("PATCH /price-lists/{id}/enable reactiva la lista")
    void patch_enable() throws Exception {
        when(reactivateUseCase.execute(1L)).thenReturn(borrador());

        mockMvc.perform(patch("/price-lists/1/enable")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }
}
