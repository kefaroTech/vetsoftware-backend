package com.vetsoftware.app.goodsreceipt.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.goodsreceipt.application.command.CreateGoodsReceiptCommand;
import com.vetsoftware.app.goodsreceipt.application.command.GoodsReceiptLineCommand;
import com.vetsoftware.app.goodsreceipt.application.command.SearchGoodsReceiptsCommand;
import com.vetsoftware.app.goodsreceipt.application.dto.BranchSummaryDto;
import com.vetsoftware.app.goodsreceipt.application.dto.CompanySummaryDto;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptDto;
import com.vetsoftware.app.goodsreceipt.application.dto.GoodsReceiptLineDto;
import com.vetsoftware.app.goodsreceipt.application.dto.ProductSummaryDto;
import com.vetsoftware.app.goodsreceipt.application.dto.SupplierSummaryDto;
import com.vetsoftware.app.goodsreceipt.application.port.in.CancelGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.ConfirmGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.CreateGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.DeleteGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.FindGoodsReceiptUseCase;
import com.vetsoftware.app.goodsreceipt.application.port.in.SearchGoodsReceiptsUseCase;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptNotFoundException;
import com.vetsoftware.app.goodsreceipt.domain.GoodsReceiptStatus;
import com.vetsoftware.app.goodsreceipt.domain.InvalidGoodsReceiptStatusTransitionException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller de recepciones de mercancia: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles de los puertos de entrada.
 *
 * <p>
 * La asercion que sostiene el multi-tenant vive aqui: el {@code companyId}
 * <b>no</b> viaja en el cuerpo, lo pone {@code Authz}. Cada verificacion de
 * command comprueba que llega {@link WebMvcSliceConfig#COMPANY_ID}, y las
 * escrituras que sellan autoria, que llega
 * {@link WebMvcSliceConfig#EMPLOYEE_ID}.
 */
@WebMvcTest(GoodsReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("GoodsReceiptController — contrato HTTP")
class GoodsReceiptControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    private static final Long RECEIPT_ID = 500L;
    private static final Long BRANCH_ID = 4L;
    private static final Long SUPPLIER_ID = 7L;
    private static final Long PRODUCT_ID = 21L;

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 12);
    private static final LocalDate VENCIMIENTO = LocalDate.of(2027, 1, 31);

    private static final String CUERPO_VALIDO = """
            {"branchId":4,"supplierId":7,"receiptDate":"2026-03-12",
             "supplierInvoiceNumber":"FV-1001","notes":"Entrega parcial",
             "lines":[{"productId":21,"lotNumber":"LOTE-A","expireDate":"2027-01-31",
                       "quantityReceived":10,"unitCost":12.50}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGoodsReceiptUseCase createUseCase;
    @MockitoBean
    private FindGoodsReceiptUseCase findUseCase;
    @MockitoBean
    private SearchGoodsReceiptsUseCase searchUseCase;
    @MockitoBean
    private DeleteGoodsReceiptUseCase deleteUseCase;
    @MockitoBean
    private ConfirmGoodsReceiptUseCase confirmUseCase;
    @MockitoBean
    private CancelGoodsReceiptUseCase cancelUseCase;

    private static GoodsReceiptLineCommand comandoLinea() {
        return new GoodsReceiptLineCommand(PRODUCT_ID, null, "LOTE-A", VENCIMIENTO, 10,
                new BigDecimal("12.50"));
    }

    private static GoodsReceiptDto recepcion(GoodsReceiptStatus estado) {
        return new GoodsReceiptDto(RECEIPT_ID,
                new CompanySummaryDto(COMPANY_ID, "Clinica Norte", "NIT-900123"),
                new BranchSummaryDto(BRANCH_ID, "Sede Norte"),
                new SupplierSummaryDto(SUPPLIER_ID, "Distribuidora Vet"), null, FECHA, "FV-1001",
                "Entrega parcial", estado,
                List.of(new GoodsReceiptLineDto(300L,
                        new ProductSummaryDto(PRODUCT_ID, "Vacuna triple", "P-021"), null, "LOTE-A",
                        VENCIMIENTO, 10, new BigDecimal("12.50"))),
                LocalDateTime.of(2026, 3, 12, 9, 15), EMPLOYEE_ID, null, null, 0L, true);
    }

    private static GoodsReceiptDto borrador() {
        return recepcion(GoodsReceiptStatus.DRAFT);
    }

    @Nested
    @DisplayName("POST /goods-receipts")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la recepcion creada y sus lineas")
        void responde_201_con_la_recepcion_creada() throws Exception {
            when(createUseCase.execute(any())).thenReturn(borrador());

            mockMvc.perform(post("/goods-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900123"))
                    .andExpect(jsonPath("$.branch.name").value("Sede Norte"))
                    .andExpect(jsonPath("$.supplier.name").value("Distribuidora Vet"))
                    .andExpect(jsonPath("$.purchaseOrderId").doesNotExist())
                    .andExpect(jsonPath("$.receiptDate").value("2026-03-12"))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.lines[0].product.code").value("P-021"))
                    .andExpect(jsonPath("$.lines[0].lotNumber").value("LOTE-A"))
                    .andExpect(jsonPath("$.lines[0].quantityReceived").value(10))
                    .andExpect(jsonPath("$.lines[0].unitCost").value(12.50))
                    .andExpect(jsonPath("$.version").value(0))
                    .andExpect(jsonPath("$.enabled").value(true));
        }

        @Test
        @DisplayName("traduce el request al command tomando empresa y empleado del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any())).thenReturn(borrador());

            mockMvc.perform(post("/goods-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            // El cuerpo no lleva companyId: si el controller lo aceptara del cliente,
            // cualquiera podria recibir mercancia contra otra empresa.
            verify(createUseCase).execute(
                    new CreateGoodsReceiptCommand(BRANCH_ID, SUPPLIER_ID, null, FECHA, "FV-1001",
                            "Entrega parcial", List.of(comandoLinea()), COMPANY_ID, EMPLOYEE_ID));
        }

        @Test
        @DisplayName("una recepcion sin lineas responde 400 y no llega al caso de uso")
        void sin_lineas_responde_400() throws Exception {
            mockMvc.perform(
                    post("/goods-receipts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":4,"supplierId":7,"receiptDate":"2026-03-12","lines":[]}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin sede ni proveedor responde 400")
        void sin_sede_ni_proveedor_responde_400() throws Exception {
            mockMvc.perform(
                    post("/goods-receipts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"receiptDate":"2026-03-12",
                             "lines":[{"productId":21,"quantityReceived":10,"unitCost":12.50}]}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una linea con cantidad cero responde 400")
        void linea_con_cantidad_cero_responde_400() throws Exception {
            mockMvc.perform(
                    post("/goods-receipts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":4,"supplierId":7,"receiptDate":"2026-03-12",
                             "lines":[{"productId":21,"quantityReceived":0,"unitCost":12.50}]}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una linea con costo negativo responde 400")
        void linea_con_costo_negativo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/goods-receipts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":4,"supplierId":7,"receiptDate":"2026-03-12",
                             "lines":[{"productId":21,"quantityReceived":5,"unitCost":-1}]}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("una referencia inexistente del dominio sale como 400, no 500")
        void referencia_inexistente_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("Supplier not found: 7"));

            mockMvc.perform(post("/goods-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /goods-receipts/{id} devuelve el recurso")
        void get_por_id_devuelve_el_recurso() throws Exception {
            when(findUseCase.findById(RECEIPT_ID, COMPANY_ID)).thenReturn(borrador());

            mockMvc.perform(get("/goods-receipts/500")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.supplierInvoiceNumber").value("FV-1001"))
                    .andExpect(jsonPath("$.lines.length()").value(1));
        }

        @Test
        @DisplayName("GET /goods-receipts/{id} de otra empresa responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new GoodsReceiptNotFoundException(99L));

            mockMvc.perform(get("/goods-receipts/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /goods-receipts/search devuelve la envoltura paginada")
        void get_search_devuelve_la_envoltura_paginada() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(borrador()), 1, 5, 11L, 3));

            mockMvc.perform(get("/goods-receipts/search").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(500))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("GET /goods-receipts/search sin parametros usa pagina 0 y tamano 20")
        void get_search_sin_parametros_usa_pagina_0_y_tamano_20() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/goods-receipts/search")).andExpect(status().isOk());

            verify(searchUseCase).execute(new SearchGoodsReceiptsCommand(COMPANY_ID, null, null,
                    null, null, null, 0, 20));
        }

        @Test
        @DisplayName("GET /goods-receipts/search traslada todos los filtros al command")
        void get_search_traslada_todos_los_filtros() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/goods-receipts/search").param("supplierId", "7")
                    .param("branchId", "4").param("status", "CONFIRMED").param("from", "2026-03-01")
                    .param("to", "2026-03-31").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk());

            verify(searchUseCase).execute(new SearchGoodsReceiptsCommand(COMPANY_ID, SUPPLIER_ID,
                    BRANCH_ID, GoodsReceiptStatus.CONFIRMED, LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 3, 31), 1, 5));
        }
    }

    @Nested
    @DisplayName("escrituras sobre una recepcion existente")
    class Escrituras {

        @Test
        @DisplayName("DELETE /goods-receipts/{id} responde 204 sin cuerpo")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/goods-receipts/500")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(RECEIPT_ID, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una recepcion inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new GoodsReceiptNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    COMPANY_ID);

            mockMvc.perform(delete("/goods-receipts/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("transiciones de estado")
    class Transiciones {

        @Test
        @DisplayName("POST /{id}/confirm responde 200 con la recepcion en CONFIRMED")
        void confirm_responde_200() throws Exception {
            // El stub exige (id de la ruta, empresa del contexto, empleado del contexto):
            // si el controller sellara otro actor, el mock devolveria null y esto seria un
            // 500 en vez de un 200.
            when(confirmUseCase.execute(RECEIPT_ID, COMPANY_ID, EMPLOYEE_ID))
                    .thenReturn(recepcion(GoodsReceiptStatus.CONFIRMED));

            mockMvc.perform(post("/goods-receipts/500/confirm")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(500))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("confirmar una recepcion que no esta en DRAFT responde 409")
        void confirm_fuera_de_borrador_responde_409() throws Exception {
            when(confirmUseCase.execute(RECEIPT_ID, COMPANY_ID, EMPLOYEE_ID)).thenThrow(
                    new InvalidGoodsReceiptStatusTransitionException(GoodsReceiptStatus.CONFIRMED,
                            GoodsReceiptStatus.CONFIRMED));

            mockMvc.perform(post("/goods-receipts/500/confirm")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("confirmar una recepcion inexistente responde 404")
        void confirm_inexistente_responde_404() throws Exception {
            when(confirmUseCase.execute(99L, COMPANY_ID, EMPLOYEE_ID))
                    .thenThrow(new GoodsReceiptNotFoundException(99L));

            mockMvc.perform(post("/goods-receipts/99/confirm")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /{id}/cancel responde 200 con la recepcion en CANCELLED")
        void cancel_responde_200() throws Exception {
            when(cancelUseCase.execute(RECEIPT_ID, COMPANY_ID, EMPLOYEE_ID))
                    .thenReturn(recepcion(GoodsReceiptStatus.CANCELLED));

            mockMvc.perform(post("/goods-receipts/500/cancel")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("cancelar una recepcion que no esta CONFIRMED responde 409")
        void cancel_fuera_de_confirmada_responde_409() throws Exception {
            when(cancelUseCase.execute(RECEIPT_ID, COMPANY_ID, EMPLOYEE_ID)).thenThrow(
                    new InvalidGoodsReceiptStatusTransitionException(GoodsReceiptStatus.DRAFT,
                            GoodsReceiptStatus.CANCELLED));

            mockMvc.perform(post("/goods-receipts/500/cancel")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("cancelar una recepcion inexistente responde 404")
        void cancel_inexistente_responde_404() throws Exception {
            when(cancelUseCase.execute(99L, COMPANY_ID, EMPLOYEE_ID))
                    .thenThrow(new GoodsReceiptNotFoundException(99L));

            mockMvc.perform(post("/goods-receipts/99/cancel")).andExpect(status().isNotFound());
        }
    }
}
