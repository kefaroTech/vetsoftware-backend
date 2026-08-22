package com.vetsoftware.app.inventory.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.auth.infrastructure.security.BranchAccessDeniedException;
import com.vetsoftware.app.inventory.application.command.InventoryAlertsQuery;
import com.vetsoftware.app.inventory.application.command.InventoryValuationQuery;
import com.vetsoftware.app.inventory.application.command.ListLotsCommand;
import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import com.vetsoftware.app.inventory.application.command.RecordCountCommand;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.command.SearchCountsQuery;
import com.vetsoftware.app.inventory.application.command.SearchKardexCommand;
import com.vetsoftware.app.inventory.application.command.SearchPurchasesQuery;
import com.vetsoftware.app.inventory.application.command.SearchStockCommand;
import com.vetsoftware.app.inventory.application.command.SetMinStockCommand;
import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import com.vetsoftware.app.inventory.application.dto.ExpiringLotView;
import com.vetsoftware.app.inventory.application.dto.InventoryAlertsView;
import com.vetsoftware.app.inventory.application.dto.InventoryCountLineView;
import com.vetsoftware.app.inventory.application.dto.InventoryCountView;
import com.vetsoftware.app.inventory.application.dto.InventoryValuationView;
import com.vetsoftware.app.inventory.application.dto.KardexReport;
import com.vetsoftware.app.inventory.application.dto.KardexReportLine;
import com.vetsoftware.app.inventory.application.dto.ProductValuationView;
import com.vetsoftware.app.inventory.application.dto.PurchaseView;
import com.vetsoftware.app.inventory.application.dto.PurchasesReport;
import com.vetsoftware.app.inventory.application.dto.StockLotView;
import com.vetsoftware.app.inventory.application.dto.StockMovementView;
import com.vetsoftware.app.inventory.application.dto.StockView;
import com.vetsoftware.app.inventory.application.port.in.AdjustStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ConsumeStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ExportInventoryUseCase;
import com.vetsoftware.app.inventory.application.port.in.GetCountUseCase;
import com.vetsoftware.app.inventory.application.port.in.GetInventoryAlertsUseCase;
import com.vetsoftware.app.inventory.application.port.in.GetInventoryValuationUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListCountsUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListKardexUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListProductLotsUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListPurchasesUseCase;
import com.vetsoftware.app.inventory.application.port.in.ListStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.ReceiveStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.RecordCountUseCase;
import com.vetsoftware.app.inventory.application.port.in.SetMinStockUseCase;
import com.vetsoftware.app.inventory.application.port.in.TransferStockUseCase;
import com.vetsoftware.app.inventory.application.port.out.InventoryReportPdfPort;
import com.vetsoftware.app.inventory.domain.InsufficientStockException;
import com.vetsoftware.app.inventory.domain.InventoryCountNotFoundException;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Rodaja HTTP del controller de inventario: rutas, binding, validacion del
 * request, codigos de estado y forma del JSON. Lo que hay debajo son dobles.
 *
 * <p>
 * Tres cosas que este controller decide por su cuenta y que por eso se afirman
 * aqui:
 *
 * <ul>
 * <li><b>La empresa nunca viaja en el cuerpo.</b> Ningun request de inventario
 * lleva {@code companyId}: lo pone {@code Authz}. Cada traduccion
 * request→command comprueba que el command sale sellado con
 * {@link WebMvcSliceConfig#COMPANY_ID}.</li>
 * <li><b>La sede tampoco se acepta tal cual.</b> Pasa por
 * {@code authz.resolveAccessibleBranch}, que la acota al alcance del empleado.
 * El doble devuelve una sede distinta de la que manda el cuerpo para que la
 * asercion pueda distinguir «vino del contexto» de «vino del cliente».</li>
 * <li><b>El destino de una transferencia es la excepcion deliberada</b>: solo
 * el origen se acota; el destino lo valida el service. Esa asimetria se
 * verifica explicitamente para que un refactor no la borre.</li>
 * </ul>
 *
 * <p>
 * La autorizacion no se prueba aqui: la cadena de seguridad esta desactivada en
 * la rodaja y el {@code @PreAuthorize} vive en los quince puertos de entrada
 * —no en este controller—, donde lo comprueba ArchUnit. Lo que si es contrato
 * HTTP y si se prueba es el 403 de {@code BranchAccessDeniedException}, que
 * nace del propio controller al resolver la sede.
 */
@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("InventoryController — contrato HTTP")
class InventoryControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    /**
     * Sede que devuelve el resolver, distinta de cualquiera que mande el cuerpo.
     */
    private static final Long SEDE_RESUELTA = 77L;
    /** Sede fuera del alcance del empleado. */
    private static final Long SEDE_AJENA = 66L;
    private static final Long PRODUCTO = 930L;
    private static final Long SEDE_DESTINO = 88L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private ListStockUseCase listStockUseCase;
    @MockitoBean
    private ListProductLotsUseCase listLotsUseCase;
    @MockitoBean
    private ListKardexUseCase listKardexUseCase;
    @MockitoBean
    private AdjustStockUseCase adjustStockUseCase;
    @MockitoBean
    private ReceiveStockUseCase receiveStockUseCase;
    @MockitoBean
    private TransferStockUseCase transferStockUseCase;
    @MockitoBean
    private SetMinStockUseCase setMinStockUseCase;
    @MockitoBean
    private ConsumeStockUseCase consumeStockUseCase;
    @MockitoBean
    private GetInventoryAlertsUseCase alertsUseCase;
    @MockitoBean
    private GetInventoryValuationUseCase valuationUseCase;
    @MockitoBean
    private ListPurchasesUseCase listPurchasesUseCase;
    @MockitoBean
    private RecordCountUseCase recordCountUseCase;
    @MockitoBean
    private ListCountsUseCase listCountsUseCase;
    @MockitoBean
    private GetCountUseCase getCountUseCase;
    @MockitoBean
    private ExportInventoryUseCase exportInventoryUseCase;
    /**
     * El controller inyecta este puerto de salida directamente para el export en
     * PDF, asi que la rodaja tiene que doblarlo aunque no sea un {@code port/in}.
     */
    @MockitoBean
    private InventoryReportPdfPort inventoryReportPdf;

    /**
     * {@code WebMvcSliceConfig} no stubea el alcance de sedes. Sin esto,
     * {@code resolveAccessibleBranch} devolveria 0L —el default de Mockito para un
     * {@code Long}, no null— y el command llegaria firmado con una sede que no
     * existe, dejando pasar justo el bug que estos tests buscan.
     */
    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        when(authz.resolveAccessibleBranch(any())).thenReturn(SEDE_RESUELTA);
    }

    private static StockView saldo() {
        return new StockView(PRODUCTO, "Amoxicilina 500mg", "SKU-100", SEDE_RESUELTA, "Sede Centro",
                12, 20, true);
    }

    private static InventoryCountView conteo() {
        return new InventoryCountView(31L, SEDE_RESUELTA, "conteo mensual", EMPLOYEE_ID,
                LocalDateTime.of(2026, 1, 15, 10, 30), 2, 1,
                List.of(new InventoryCountLineView(PRODUCTO, 10, 7, -3)));
    }

    private static KardexReport kardexReport() {
        return new KardexReport("Amoxicilina 500mg", "SKU-100", "Sede Centro",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                LocalDateTime.of(2026, 2, 1, 8, 0), 5, 12,
                List.of(new KardexReportLine(LocalDateTime.of(2026, 1, 10, 9, 0), "Compra",
                        "Recepcion 4001", 700L, 7, new BigDecimal("1500.0000"), 12)));
    }

    private static PurchasesReport purchasesReport() {
        return new PurchasesReport("Sede Centro", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), LocalDateTime.of(2026, 2, 1, 8, 0),
                List.of(new PurchaseView(1L, PRODUCTO, "Amoxicilina 500mg", "SKU-100", 700L,
                        SEDE_RESUELTA, "Sede Centro", 7, new BigDecimal("1500.0000"),
                        new BigDecimal("10500.0000"), 4001L, LocalDateTime.of(2026, 1, 10, 9, 0))),
                7, new BigDecimal("10500.0000"));
    }

    @Nested
    @DisplayName("lecturas del saldo y del kardex")
    class Lecturas {

        @Test
        @DisplayName("GET /inventory/stock devuelve la envoltura paginada")
        void get_stock_devuelve_la_envoltura_paginada() throws Exception {
            when(listStockUseCase.search(any()))
                    .thenReturn(new PageResult<>(List.of(saldo()), 1, 5, 11L, 3));

            mockMvc.perform(get("/inventory/stock").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productName").value("Amoxicilina 500mg"))
                    .andExpect(jsonPath("$.content[0].lowStock").value(true))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("GET /inventory/stock sin parametros usa pagina 0, tamano 20 y no filtra")
        void get_stock_sin_parametros_usa_los_defectos() throws Exception {
            when(listStockUseCase.search(any())).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/inventory/stock")).andExpect(status().isOk());

            verify(listStockUseCase).search(new SearchStockCommand(COMPANY_ID, SEDE_RESUELTA, null,
                    false, List.of(), 0, 20));
        }

        @Test
        @DisplayName("GET /inventory/stock traslada texto y bajo minimo al command")
        void get_stock_traslada_los_filtros() throws Exception {
            when(listStockUseCase.search(any())).thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/inventory/stock").param("q", "amox").param("lowStock", "true"))
                    .andExpect(status().isOk());

            verify(listStockUseCase).search(new SearchStockCommand(COMPANY_ID, SEDE_RESUELTA,
                    "amox", true, List.of(), 0, 20));
        }

        @Test
        @DisplayName("GET /inventory/products/{id}/lots devuelve los lotes del producto")
        void get_lotes() throws Exception {
            when(listLotsUseCase.listLots(any())).thenReturn(List.of(new StockLotView(700L,
                    "L-2026-01", LocalDate.of(2027, 1, 31), 7, new BigDecimal("1500.0000"))));

            mockMvc.perform(get("/inventory/products/930/lots")).andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].lotId").value(700))
                    .andExpect(jsonPath("$[0].lotNumber").value("L-2026-01"))
                    .andExpect(jsonPath("$[0].expireDate").value("2027-01-31"))
                    .andExpect(jsonPath("$[0].quantityAvailable").value(7));

            verify(listLotsUseCase)
                    .listLots(new ListLotsCommand(COMPANY_ID, SEDE_RESUELTA, PRODUCTO));
        }

        @Test
        @DisplayName("GET /inventory/products/{id}/kardex traduce las fechas ISO al command")
        void get_kardex_traduce_las_fechas() throws Exception {
            when(listKardexUseCase.searchKardex(any()))
                    .thenReturn(new PageResult<>(
                            List.of(new StockMovementView(1L, 700L, "PURCHASE", 7,
                                    new BigDecimal("1500.0000"), "GOODS_RECEIPT", 4001L, "compra",
                                    EMPLOYEE_ID, LocalDateTime.of(2026, 1, 10, 9, 0))),
                            0, 20, 1L, 1));

            mockMvc.perform(get("/inventory/products/930/kardex").param("from", "2026-01-01")
                    .param("to", "2026-01-31")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].type").value("PURCHASE"))
                    .andExpect(jsonPath("$.content[0].quantity").value(7))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listKardexUseCase)
                    .searchKardex(new SearchKardexCommand(COMPANY_ID, SEDE_RESUELTA, PRODUCTO,
                            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 0, 20));
        }

        @Test
        @DisplayName("GET /inventory/alerts aplica treinta dias de ventana por defecto")
        void get_alertas_usa_treinta_dias_por_defecto() throws Exception {
            when(alertsUseCase.alerts(any())).thenReturn(new InventoryAlertsView(List.of(saldo()),
                    List.of(new ExpiringLotView(PRODUCTO, "Amoxicilina 500mg", "SKU-100",
                            SEDE_RESUELTA, "Sede Centro", 700L, "L-2026-01",
                            LocalDate.of(2026, 2, 1), 7, 12L))));

            mockMvc.perform(get("/inventory/alerts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.lowStock[0].productCode").value("SKU-100"))
                    .andExpect(jsonPath("$.expiring[0].daysToExpire").value(12));

            verify(alertsUseCase).alerts(new InventoryAlertsQuery(COMPANY_ID, SEDE_RESUELTA, 30));
        }

        @Test
        @DisplayName("GET /inventory/valuation devuelve el valor y las unidades")
        void get_valuacion() throws Exception {
            when(valuationUseCase.valuation(any())).thenReturn(new InventoryValuationView(
                    new BigDecimal("10500.0000"), 7, List.of(new ProductValuationView(PRODUCTO,
                            "Amoxicilina 500mg", "SKU-100", 7, new BigDecimal("10500.0000")))));

            mockMvc.perform(get("/inventory/valuation")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUnits").value(7))
                    .andExpect(jsonPath("$.byProduct[0].productCode").value("SKU-100"));

            verify(valuationUseCase)
                    .valuation(new InventoryValuationQuery(COMPANY_ID, SEDE_RESUELTA));
        }

        @Test
        @DisplayName("GET /inventory/purchases devuelve el libro de compras paginado")
        void get_compras() throws Exception {
            when(listPurchasesUseCase.purchases(any()))
                    .thenReturn(new PageResult<>(purchasesReport().lines(), 0, 20, 1L, 1));

            mockMvc.perform(get("/inventory/purchases")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].productName").value("Amoxicilina 500mg"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listPurchasesUseCase).purchases(
                    new SearchPurchasesQuery(COMPANY_ID, SEDE_RESUELTA, null, null, 0, 20));
        }
    }

    @Nested
    @DisplayName("movimientos de stock")
    class Movimientos {

        @Test
        @DisplayName("POST /inventory/consumptions responde 204 y sella empresa, sede y autor")
        void post_consumo_responde_204() throws Exception {
            mockMvc.perform(post("/inventory/consumptions").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"branchId":911,"productId":930,"quantity":3,"reason":"Cirugia"}
                            """)).andExpect(status().isNoContent());

            verify(consumeStockUseCase).consume(new RecordClinicalUseCommand(COMPANY_ID,
                    SEDE_RESUELTA, PRODUCTO, 3, null, "Cirugia", EMPLOYEE_ID));
        }

        @Test
        @DisplayName("POST /inventory/consumptions con cantidad cero responde 400")
        void post_consumo_con_cantidad_cero_responde_400() throws Exception {
            mockMvc.perform(post("/inventory/consumptions").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId":930,"quantity":0}
                            """)).andExpect(status().isBadRequest());

            verify(consumeStockUseCase, never()).consume(any());
        }

        @Test
        @DisplayName("POST /inventory/adjustments admite delta negativo y no inventa referencia")
        void post_ajuste_responde_204() throws Exception {
            mockMvc.perform(post("/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"branchId":911,"productId":930,"delta":-4,"unitCost":1500.00,
                             "reason":"Merma por rotura"}
                            """)).andExpect(status().isNoContent());

            verify(adjustStockUseCase)
                    .adjust(new RecordAdjustmentCommand(COMPANY_ID, SEDE_RESUELTA, PRODUCTO, -4,
                            new BigDecimal("1500.00"), "Merma por rotura", null, EMPLOYEE_ID));
        }

        @Test
        @DisplayName("POST /inventory/adjustments sin motivo responde 400")
        void post_ajuste_sin_motivo_responde_400() throws Exception {
            // Un ajuste sin motivo es un descuadre sin rastro: la validacion tiene que
            // pararlo antes del caso de uso.
            mockMvc.perform(post("/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId":930,"delta":-4,"reason":"   "}
                            """)).andExpect(status().isBadRequest());

            verify(adjustStockUseCase, never()).adjust(any());
        }

        @Test
        @DisplayName("POST /inventory/receipts marca la referencia como GOODS_RECEIPT")
        void post_recepcion_responde_204() throws Exception {
            mockMvc.perform(
                    post("/inventory/receipts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":911,"productId":930,"quantity":7,"unitCost":1500.00,
                             "lotNumber":"L-2026-01","expireDate":"2027-01-31"}
                            """)).andExpect(status().isNoContent());

            verify(receiveStockUseCase).receive(new RecordPurchaseCommand(COMPANY_ID, SEDE_RESUELTA,
                    PRODUCTO, "L-2026-01", LocalDate.of(2027, 1, 31), 7, new BigDecimal("1500.00"),
                    StockReferenceType.GOODS_RECEIPT, null, EMPLOYEE_ID));
        }

        @Test
        @DisplayName("POST /inventory/receipts con costo negativo responde 400")
        void post_recepcion_con_costo_negativo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/inventory/receipts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"productId":930,"quantity":7,"unitCost":-1}
                            """)).andExpect(status().isBadRequest());

            verify(receiveStockUseCase, never()).receive(any());
        }

        @Test
        @DisplayName("POST /inventory/transfers acota el origen pero no el destino")
        void post_transferencia_acota_solo_el_origen() throws Exception {
            // Asimetria deliberada: el origen se limita al alcance del empleado y el
            // destino puede ser cualquier sede de la empresa, que valida el service.
            // Si el destino tambien pasara por el resolver, transferir a una sede que el
            // empleado no atiende —lo normal— seria imposible.
            mockMvc.perform(
                    post("/inventory/transfers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fromBranchId":910,"toBranchId":88,"productId":930,"quantity":4,
                             "reason":"Reposicion"}
                            """)).andExpect(status().isNoContent());

            verify(transferStockUseCase).transfer(new TransferStockCommand(COMPANY_ID,
                    SEDE_RESUELTA, SEDE_DESTINO, PRODUCTO, 4, "Reposicion", EMPLOYEE_ID));
        }

        @Test
        @DisplayName("POST /inventory/transfers sin sede destino responde 400")
        void post_transferencia_sin_destino_responde_400() throws Exception {
            mockMvc.perform(
                    post("/inventory/transfers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fromBranchId":910,"productId":930,"quantity":4}
                            """)).andExpect(status().isBadRequest());

            verify(transferStockUseCase, never()).transfer(any());
        }

        @Test
        @DisplayName("PUT /inventory/products/{id}/min-stock toma el producto de la ruta")
        void put_minimo_responde_204() throws Exception {
            mockMvc.perform(put("/inventory/products/930/min-stock")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":911,"minStock":20}
                            """)).andExpect(status().isNoContent());

            verify(setMinStockUseCase)
                    .setMinStock(new SetMinStockCommand(COMPANY_ID, SEDE_RESUELTA, PRODUCTO, 20));
        }

        @Test
        @DisplayName("PUT /inventory/products/{id}/min-stock con minimo negativo responde 400")
        void put_minimo_negativo_responde_400() throws Exception {
            mockMvc.perform(put("/inventory/products/930/min-stock")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"minStock":-1}
                            """)).andExpect(status().isBadRequest());

            verify(setMinStockUseCase, never()).setMinStock(any());
        }
    }

    @Nested
    @DisplayName("conteo fisico")
    class Conteos {

        @Test
        @DisplayName("POST /inventory/counts responde 201 con las diferencias aplicadas")
        void post_conteo_responde_201() throws Exception {
            when(recordCountUseCase.record(any())).thenReturn(conteo());

            mockMvc.perform(
                    post("/inventory/counts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":911,"note":"conteo mensual",
                             "lines":[{"productId":930,"countedQuantity":7}]}
                            """)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.totalLines").value(2))
                    .andExpect(jsonPath("$.adjustedLines").value(1))
                    .andExpect(jsonPath("$.lines[0].difference").value(-3));
        }

        @Test
        @DisplayName("POST /inventory/counts traduce la hoja de conteo linea a linea")
        void post_conteo_traduce_las_lineas() throws Exception {
            when(recordCountUseCase.record(any())).thenReturn(conteo());

            mockMvc.perform(
                    post("/inventory/counts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":911,"note":"conteo mensual",
                             "lines":[{"productId":930,"countedQuantity":7},
                                      {"productId":931,"countedQuantity":0}]}
                            """));

            verify(recordCountUseCase).record(new RecordCountCommand(COMPANY_ID, SEDE_RESUELTA,
                    "conteo mensual", EMPLOYEE_ID, List.of(new RecordCountCommand.Line(PRODUCTO, 7),
                            new RecordCountCommand.Line(931L, 0))));
        }

        @Test
        @DisplayName("POST /inventory/counts sin lineas responde 400")
        void post_conteo_sin_lineas_responde_400() throws Exception {
            mockMvc.perform(
                    post("/inventory/counts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":911,"lines":[]}
                            """)).andExpect(status().isBadRequest());

            verify(recordCountUseCase, never()).record(any());
        }

        @Test
        @DisplayName("POST /inventory/counts con una cantidad contada negativa responde 400")
        void post_conteo_con_cantidad_negativa_responde_400() throws Exception {
            // La validacion tiene que entrar en cada linea (@Valid sobre la lista): sin
            // ella, un -1 llegaria al ledger como un ajuste inventado.
            mockMvc.perform(
                    post("/inventory/counts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":911,"lines":[{"productId":930,"countedQuantity":-1}]}
                            """)).andExpect(status().isBadRequest());

            verify(recordCountUseCase, never()).record(any());
        }

        @Test
        @DisplayName("POST /inventory/counts sin sede responde 400")
        void post_conteo_sin_sede_responde_400() throws Exception {
            // Es el unico request de inventario donde la sede es obligatoria: un conteo
            // sin sede no se puede contrastar contra ningun saldo.
            mockMvc.perform(
                    post("/inventory/counts").contentType(MediaType.APPLICATION_JSON).content("""
                            {"lines":[{"productId":930,"countedQuantity":7}]}
                            """)).andExpect(status().isBadRequest());

            verify(recordCountUseCase, never()).record(any());
        }

        @Test
        @DisplayName("GET /inventory/counts devuelve el historial paginado")
        void get_conteos() throws Exception {
            when(listCountsUseCase.list(any()))
                    .thenReturn(new PageResult<>(List.of(conteo()), 0, 20, 1L, 1));

            mockMvc.perform(get("/inventory/counts")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(31))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(listCountsUseCase).list(new SearchCountsQuery(COMPANY_ID, SEDE_RESUELTA, 0, 20));
        }

        @Test
        @DisplayName("GET /inventory/counts/{id} devuelve el detalle de la empresa del contexto")
        void get_conteo_por_id() throws Exception {
            when(getCountUseCase.get(COMPANY_ID, 31L)).thenReturn(conteo());

            mockMvc.perform(get("/inventory/counts/31")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.note").value("conteo mensual"))
                    .andExpect(jsonPath("$.lines[0].productId").value(930));
        }

        @Test
        @DisplayName("GET /inventory/counts/{id} inexistente responde 404, no 500")
        void get_conteo_inexistente_responde_404() throws Exception {
            when(getCountUseCase.get(COMPANY_ID, 99L))
                    .thenThrow(new InventoryCountNotFoundException(99L));

            mockMvc.perform(get("/inventory/counts/99")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("conflictos y alcance de sede")
    class Conflictos {

        @Test
        @DisplayName("un consumo sin existencias responde 409, no 500")
        void consumo_sin_existencias_responde_409() throws Exception {
            // El front distingue "sin existencias" del resto de conflictos para poder
            // ofrecer el ajuste. Si esto saliera como 500, el usuario veria un error
            // generico ante una situacion de negocio normal.
            doThrow(new InsufficientStockException("Stock insuficiente para el producto 930"))
                    .when(consumeStockUseCase).consume(any());

            mockMvc.perform(post("/inventory/consumptions").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"productId":930,"quantity":3}
                            """)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("una transferencia sin existencias en origen responde 409")
        void transferencia_sin_existencias_responde_409() throws Exception {
            doThrow(new InsufficientStockException("Stock insuficiente para el producto 930"))
                    .when(transferStockUseCase).transfer(any());

            mockMvc.perform(
                    post("/inventory/transfers").contentType(MediaType.APPLICATION_JSON).content("""
                            {"fromBranchId":910,"toBranchId":88,"productId":930,"quantity":400}
                            """)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("consultar el saldo de una sede fuera del alcance responde 403")
        void consultar_una_sede_ajena_responde_403() throws Exception {
            doThrow(new BranchAccessDeniedException("Branch not allowed for employee: 66"))
                    .when(authz).resolveAccessibleBranch(SEDE_AJENA);

            mockMvc.perform(get("/inventory/stock").param("branchId", "66"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(listStockUseCase);
        }

        @Test
        @DisplayName("ajustar en una sede fuera del alcance responde 403 y no escribe nada")
        void ajustar_en_una_sede_ajena_no_escribe() throws Exception {
            // La mitad del valor de esta prueba es el verifyNoInteractions: el 403 sin
            // escritura es lo que separa un gate real de un mensaje de error.
            doThrow(new BranchAccessDeniedException("Branch not allowed for employee: 66"))
                    .when(authz).resolveAccessibleBranch(SEDE_AJENA);

            mockMvc.perform(post("/inventory/adjustments").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"branchId":66,"productId":930,"delta":-4,"reason":"Merma"}
                            """)).andExpect(status().isForbidden());

            verifyNoInteractions(adjustStockUseCase);
        }
    }

    @Nested
    @DisplayName("exportacion del kardex y del libro de compras")
    class Exportacion {

        @Test
        @DisplayName("el kardex sale en CSV por defecto, sin paginar y sin tocar el motor de PDF")
        void export_kardex_por_defecto_es_csv() throws Exception {
            when(exportInventoryUseCase.kardexReport(any())).thenReturn(kardexReport());

            mockMvc.perform(get("/inventory/products/930/kardex/export")).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString("kardex_930.csv")));

            // page y pageSize a cero: la exportacion trae el rango completo.
            verify(exportInventoryUseCase).kardexReport(
                    new SearchKardexCommand(COMPANY_ID, SEDE_RESUELTA, PRODUCTO, null, null, 0, 0));
            verifyNoInteractions(inventoryReportPdf);
        }

        @Test
        @DisplayName("con format=pdf el cuerpo es el que devuelve el motor de PDF")
        void export_kardex_en_pdf() throws Exception {
            when(exportInventoryUseCase.kardexReport(any())).thenReturn(kardexReport());
            when(inventoryReportPdf.renderKardex(any())).thenReturn(new byte[]{1, 2, 3});

            mockMvc.perform(get("/inventory/products/930/kardex/export").param("format", "pdf"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(content().bytes(new byte[]{1, 2, 3})).andExpect(header().string(
                            HttpHeaders.CONTENT_DISPOSITION, containsString("kardex_930.pdf")));
        }

        @Test
        @DisplayName("el formato no distingue mayusculas: PDF tambien es PDF")
        void export_compras_en_pdf_en_mayusculas() throws Exception {
            when(exportInventoryUseCase.purchasesReport(any())).thenReturn(purchasesReport());
            when(inventoryReportPdf.renderPurchases(any())).thenReturn(new byte[]{9});

            mockMvc.perform(get("/inventory/purchases/export").param("format", "PDF"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString("compras.pdf")));
        }

        @Test
        @DisplayName("un formato desconocido cae al CSV en lugar de fallar")
        void export_compras_con_formato_desconocido_cae_a_csv() throws Exception {
            when(exportInventoryUseCase.purchasesReport(any())).thenReturn(purchasesReport());

            mockMvc.perform(get("/inventory/purchases/export").param("format", "xlsx"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            containsString("compras.csv")));

            verifyNoInteractions(inventoryReportPdf);
        }
    }
}
