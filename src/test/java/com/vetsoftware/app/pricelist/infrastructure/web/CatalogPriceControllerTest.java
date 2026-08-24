package com.vetsoftware.app.pricelist.infrastructure.web;

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

import com.vetsoftware.app.pricelist.application.command.CreateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.command.UpdateCatalogPriceCommand;
import com.vetsoftware.app.pricelist.application.dto.CatalogPriceDto;
import com.vetsoftware.app.pricelist.application.port.in.CreateCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.in.DeleteCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.in.FindCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.application.port.in.ListCatalogPricesUseCase;
import com.vetsoftware.app.pricelist.application.port.in.UpdateCatalogPriceUseCase;
import com.vetsoftware.app.pricelist.domain.BillingCycle;
import com.vetsoftware.app.pricelist.domain.TaxTreatment;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
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

@WebMvcTest(CatalogPriceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("CatalogPriceController — contrato HTTP")
class CatalogPriceControllerTest {

    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCatalogPriceUseCase createUseCase;
    @MockitoBean
    private UpdateCatalogPriceUseCase updateUseCase;
    @MockitoBean
    private FindCatalogPriceUseCase findUseCase;
    @MockitoBean
    private ListCatalogPricesUseCase listUseCase;
    @MockitoBean
    private DeleteCatalogPriceUseCase deleteUseCase;

    private static CatalogPriceDto gravado() {
        return new CatalogPriceDto(10L, 1L, 42L, BillingCycle.MONTHLY, 1, 10, 2,
                new BigDecimal("12000.00"), new BigDecimal("0.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED, CREADO_EL, true);
    }

    private static CatalogPriceDto excluido() {
        return new CatalogPriceDto(11L, 1L, 42L, BillingCycle.ANNUAL, 1, null, 0,
                new BigDecimal("120000.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                TaxTreatment.EXCLUDED, CREADO_EL, true);
    }

    private static final String CUERPO_VALIDO = """
            {"priceListId":1,"catalogItemId":42,"billingCycle":"MONTHLY","tierMin":1,"tierMax":10,
             "includedQuantity":2,"unitAmount":12000.00,"setupAmount":0.00,"taxRate":19.00,
             "taxTreatment":"TAXED"}
            """;

    @Test
    @DisplayName("POST /catalog-prices responde 201 con el precio creado")
    void post_responde_201() throws Exception {
        when(createUseCase.execute(any())).thenReturn(gravado());

        mockMvc.perform(post("/catalog-prices").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.billingCycle").value("MONTHLY"))
                .andExpect(jsonPath("$.taxTreatment").value("TAXED"))
                .andExpect(jsonPath("$.unitAmount").value(12000.00));
    }

    @Test
    @DisplayName("POST /catalog-prices traduce el request al command sin perder el tramo")
    void post_traduce_el_request_al_command() throws Exception {
        when(createUseCase.execute(any())).thenReturn(gravado());

        mockMvc.perform(post("/catalog-prices").contentType(MediaType.APPLICATION_JSON)
                .content(CUERPO_VALIDO));

        verify(createUseCase).execute(new CreateCatalogPriceCommand(1L, 42L, BillingCycle.MONTHLY,
                1, 10, 2, new BigDecimal("12000.00"), new BigDecimal("0.00"),
                new BigDecimal("19.00"), TaxTreatment.TAXED));
    }

    @Test
    @DisplayName("POST /catalog-prices con tramo mínimo 0 responde 400 y no llega al caso de uso")
    void post_con_tramo_cero_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-prices").contentType(MediaType.APPLICATION_JSON).content("""
                {"priceListId":1,"catalogItemId":42,"billingCycle":"MONTHLY","tierMin":0,
                 "includedQuantity":0,"unitAmount":1.00,"setupAmount":0.00,"taxRate":0.00,
                 "taxTreatment":"EXCLUDED"}
                """)).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /catalog-prices con tarifa por encima de 100 responde 400")
    void post_con_tarifa_fuera_de_rango_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-prices").contentType(MediaType.APPLICATION_JSON).content("""
                {"priceListId":1,"catalogItemId":42,"billingCycle":"MONTHLY","tierMin":1,
                 "includedQuantity":0,"unitAmount":1.00,"setupAmount":0.00,"taxRate":101.00,
                 "taxTreatment":"TAXED"}
                """)).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("POST /catalog-prices sin tratamiento fiscal responde 400: tarifa cero es ambiguo")
    void post_sin_tratamiento_fiscal_responde_400() throws Exception {
        mockMvc.perform(post("/catalog-prices").contentType(MediaType.APPLICATION_JSON).content("""
                {"priceListId":1,"catalogItemId":42,"billingCycle":"MONTHLY","tierMin":1,
                 "includedQuantity":0,"unitAmount":1.00,"setupAmount":0.00,"taxRate":0.00}
                """)).andExpect(status().isBadRequest());

        verify(createUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("GET /catalog-prices devuelve el contrato único de paginación acotado por lista")
    void get_devuelve_el_contrato_de_paginacion() throws Exception {
        when(listUseCase.listByPriceList(1L, 0, 20))
                .thenReturn(new PageResult<>(List.of(gravado(), excluido()), 0, 20, 2L, 1));

        mockMvc.perform(get("/catalog-prices").param("priceListId", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[1].taxTreatment").value("EXCLUDED"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    @DisplayName("GET /catalog-prices sin priceListId responde 400")
    void get_sin_lista_responde_400() throws Exception {
        mockMvc.perform(get("/catalog-prices")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /catalog-prices/{id} distingue EXCLUDED de una tarifa cero cualquiera")
    void get_por_id_distingue_el_tratamiento() throws Exception {
        when(findUseCase.findById(11L)).thenReturn(excluido());

        mockMvc.perform(get("/catalog-prices/11")).andExpect(status().isOk())
                .andExpect(jsonPath("$.taxTreatment").value("EXCLUDED"))
                .andExpect(jsonPath("$.taxRate").value(0.00))
                .andExpect(jsonPath("$.tierMax").doesNotExist());
    }

    @Test
    @DisplayName("PUT /catalog-prices/{id} toma el id de la URL y no reapunta lista ni artículo")
    void put_no_reapunta_el_ambito() throws Exception {
        when(updateUseCase.execute(any())).thenReturn(gravado());

        mockMvc.perform(
                put("/catalog-prices/10").contentType(MediaType.APPLICATION_JSON).content("""
                        {"billingCycle":"MONTHLY","tierMin":1,"tierMax":10,"includedQuantity":2,
                         "unitAmount":9000.00,"setupAmount":0.00,"taxRate":19.00,
                         "taxTreatment":"TAXED"}
                        """)).andExpect(status().isOk());

        verify(updateUseCase).execute(new UpdateCatalogPriceCommand(10L, BillingCycle.MONTHLY, 1,
                10, 2, new BigDecimal("9000.00"), new BigDecimal("0.00"), new BigDecimal("19.00"),
                TaxTreatment.TAXED));
    }

    @Test
    @DisplayName("DELETE /catalog-prices/{id} responde 204")
    void delete_responde_204() throws Exception {
        mockMvc.perform(delete("/catalog-prices/10")).andExpect(status().isNoContent());

        verify(deleteUseCase).execute(10L);
    }
}
