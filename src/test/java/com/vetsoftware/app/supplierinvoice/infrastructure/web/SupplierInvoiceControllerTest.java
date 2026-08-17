package com.vetsoftware.app.supplierinvoice.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.auth.infrastructure.security.BranchAccessDeniedException;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.supplierinvoice.application.command.CreateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.command.RegisterSupplierPaymentCommand;
import com.vetsoftware.app.supplierinvoice.application.command.SearchSupplierInvoicesCommand;
import com.vetsoftware.app.supplierinvoice.application.command.UpdateSupplierInvoiceCommand;
import com.vetsoftware.app.supplierinvoice.application.port.in.CancelSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.CreateSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.DeleteSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.FindSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.GetAccountsPayableAgingUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.RegisterSupplierPaymentUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.SearchSupplierInvoicesUseCase;
import com.vetsoftware.app.supplierinvoice.application.port.in.UpdateSupplierInvoiceUseCase;
import com.vetsoftware.app.supplierinvoice.domain.InvalidSupplierInvoiceStateException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNotFoundException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceNumberAlreadyExistsException;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoicePaymentMethod;
import com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceStatus;
import com.vetsoftware.app.supplierinvoice.testsupport.SupplierInvoiceMother;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * Rodaja HTTP del controller de facturas de proveedor: rutas, binding,
 * validacion del request, codigos de estado y forma del JSON. Lo que hay debajo
 * son dobles.
 *
 * <p>
 * Lo que mas se vigila aqui es que ni la empresa ni la sede vengan del cliente:
 * el {@code companyId} lo pone {@code Authz} y la sede la resuelve
 * {@code resolveAccessibleBranch}. Un request que pudiera elegir empresa seria
 * una fuga entre tenants.
 */
@WebMvcTest(SupplierInvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("SupplierInvoiceController — contrato HTTP")
class SupplierInvoiceControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;

    /** Sede que pide el request y que si esta en el alcance del empleado. */
    private static final Long BRANCH_ID = 3L;
    /** Sede que resuelve Authz cuando el request no manda ninguna. */
    private static final Long BRANCH_POR_DEFECTO = 8L;
    /** Sede fuera del alcance del empleado. */
    private static final Long BRANCH_AJENA = 77L;

    private static final String CUERPO_VALIDO = """
            {"branchId":3,"supplierId":7,"purchaseOrderId":31,"goodsReceiptId":41,
             "invoiceNumber":"FAC-001","issueDate":"2026-01-10","dueDate":"2026-02-09",
             "subtotal":1000000.00,"taxAmount":190000.00,"withholdingAmount":25000.00,
             "notes":"Compra de insumos"}
            """;

    private static final String CUERPO_VALIDO_UPDATE = """
            {"branchId":3,"supplierId":7,"purchaseOrderId":31,"goodsReceiptId":41,
             "invoiceNumber":"FAC-001","issueDate":"2026-01-10","dueDate":"2026-02-09",
             "subtotal":1000000.00,"taxAmount":190000.00,"withholdingAmount":25000.00,
             "notes":"Compra de insumos","version":1}
            """;

    private static final String CUERPO_ABONO = """
            {"amount":165000.00,"paymentDate":"2026-02-01","method":"TRANSFER",
             "reference":"TRF-9","note":"Abono parcial","version":1}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Authz authz;

    @MockitoBean
    private CreateSupplierInvoiceUseCase createUseCase;
    @MockitoBean
    private UpdateSupplierInvoiceUseCase updateUseCase;
    @MockitoBean
    private FindSupplierInvoiceUseCase findUseCase;
    @MockitoBean
    private SearchSupplierInvoicesUseCase searchUseCase;
    @MockitoBean
    private RegisterSupplierPaymentUseCase registerPaymentUseCase;
    @MockitoBean
    private CancelSupplierInvoiceUseCase cancelUseCase;
    @MockitoBean
    private DeleteSupplierInvoiceUseCase deleteUseCase;
    @MockitoBean
    private GetAccountsPayableAgingUseCase agingUseCase;

    @BeforeEach
    void laSedeLaResuelveAuthz() {
        // lenient: solo cinco de los ocho endpoints resuelven sede; en los otros
        // (ver por id, anular, borrar) el stub quedaria sin usar.
        lenient().when(authz.resolveAccessibleBranch(BRANCH_ID)).thenReturn(BRANCH_ID);
        lenient().when(authz.resolveAccessibleBranch(null)).thenReturn(BRANCH_POR_DEFECTO);
    }

    @Nested
    @DisplayName("POST /supplier-invoices")
    class Creacion {

        @Test
        @DisplayName("responde 201 con la factura y su saldo")
        void responde_201_con_la_factura_y_su_saldo() throws Exception {
            when(createUseCase.execute(any()))
                    .thenReturn(SupplierInvoiceMother.dtoParcial(COMPANY_ID));

            mockMvc.perform(post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.invoiceNumber").value("FAC-001"))
                    .andExpect(jsonPath("$.company.identifier").value("NIT-900"))
                    .andExpect(jsonPath("$.branch.name").value("Sede Centro"))
                    .andExpect(jsonPath("$.supplier.taxId").value("800111222"))
                    .andExpect(jsonPath("$.total").value(1190000.00))
                    .andExpect(jsonPath("$.payableAmount").value(1165000.00))
                    .andExpect(jsonPath("$.paidAmount").value(165000.00))
                    .andExpect(jsonPath("$.balance").value(1000000.00))
                    .andExpect(jsonPath("$.status").value("PARTIAL"))
                    .andExpect(jsonPath("$.payments[0].method").value("TRANSFER"))
                    .andExpect(jsonPath("$.payments[0].reference").value("TRF-9"))
                    .andExpect(jsonPath("$.version").value(1));
        }

        @Test
        @DisplayName("traduce el request al command con la empresa y el autor del contexto")
        void traduce_el_request_al_command() throws Exception {
            when(createUseCase.execute(any()))
                    .thenReturn(SupplierInvoiceMother.dtoParcial(COMPANY_ID));

            mockMvc.perform(post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO));

            // El companyId NO viaja en el request: lo sella Authz.
            verify(createUseCase).execute(new CreateSupplierInvoiceCommand(BRANCH_ID, 7L, 31L, 41L,
                    "FAC-001", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                    new BigDecimal("1000000.00"), new BigDecimal("190000.00"),
                    new BigDecimal("25000.00"), "Compra de insumos", COMPANY_ID, EMPLOYEE_ID));
        }

        @Test
        @DisplayName("sin sede en el request la resuelve Authz, no se queda en null")
        void sin_sede_en_el_request_la_resuelve_authz() throws Exception {
            when(createUseCase.execute(any()))
                    .thenReturn(SupplierInvoiceMother.dtoParcial(COMPANY_ID));

            mockMvc.perform(
                    post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON).content("""
                            {"supplierId":7,"invoiceNumber":"FAC-001","issueDate":"2026-01-10",
                             "dueDate":"2026-02-09","subtotal":1000000.00,"taxAmount":190000.00,
                             "withholdingAmount":25000.00}
                            """));

            verify(createUseCase).execute(new CreateSupplierInvoiceCommand(BRANCH_POR_DEFECTO, 7L,
                    null, null, "FAC-001", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                    new BigDecimal("1000000.00"), new BigDecimal("190000.00"),
                    new BigDecimal("25000.00"), null, COMPANY_ID, EMPLOYEE_ID));
        }

        @Test
        @DisplayName("una sede fuera del alcance responde 403 y no llega al caso de uso")
        void sede_fuera_del_alcance_responde_403() throws Exception {
            when(authz.resolveAccessibleBranch(BRANCH_AJENA)).thenThrow(
                    new BranchAccessDeniedException("Branch not allowed for employee: 77"));

            mockMvc.perform(
                    post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":77,"supplierId":7,"invoiceNumber":"FAC-001",
                             "issueDate":"2026-01-10","dueDate":"2026-02-09","subtotal":1000000.00,
                             "taxAmount":190000.00,"withholdingAmount":25000.00}
                            """)).andExpect(status().isForbidden());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin numero de factura responde 400 y no llega al caso de uso")
        void sin_numero_de_factura_responde_400() throws Exception {
            mockMvc.perform(
                    post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":3,"supplierId":7,"invoiceNumber":"  ",
                             "issueDate":"2026-01-10","dueDate":"2026-02-09","subtotal":1000000.00,
                             "taxAmount":190000.00}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("subtotal negativo responde 400")
        void subtotal_negativo_responde_400() throws Exception {
            mockMvc.perform(
                    post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON).content("""
                            {"branchId":3,"supplierId":7,"invoiceNumber":"FAC-001",
                             "issueDate":"2026-01-10","dueDate":"2026-02-09","subtotal":-1,
                             "taxAmount":190000.00}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("sin proveedor ni fechas responde 400")
        void sin_proveedor_ni_fechas_responde_400() throws Exception {
            mockMvc.perform(
                    post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON).content("""
                            {"invoiceNumber":"FAC-001","subtotal":1000000.00,"taxAmount":190000.00}
                            """)).andExpect(status().isBadRequest());

            verify(createUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un numero ya registrado para ese proveedor responde 409")
        void numero_repetido_responde_409() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new SupplierInvoiceNumberAlreadyExistsException("FAC-001"));

            mockMvc.perform(post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("un vencimiento anterior a la emision sale como 400, no 500")
        void vencimiento_invalido_responde_400() throws Exception {
            when(createUseCase.execute(any()))
                    .thenThrow(new IllegalArgumentException("dueDate cannot be before issueDate"));

            mockMvc.perform(post("/supplier-invoices").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /supplier-invoices/{id} devuelve la factura de la empresa del contexto")
        void get_por_id_devuelve_la_factura() throws Exception {
            when(findUseCase.findById(55L, COMPANY_ID))
                    .thenReturn(SupplierInvoiceMother.dtoParcial(COMPANY_ID));

            mockMvc.perform(get("/supplier-invoices/55")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55))
                    .andExpect(jsonPath("$.company.id").value(COMPANY_ID))
                    .andExpect(jsonPath("$.payments.length()").value(1));
        }

        @Test
        @DisplayName("GET /supplier-invoices/{id} inexistente responde 404, no 500")
        void get_por_id_inexistente_responde_404() throws Exception {
            when(findUseCase.findById(99L, COMPANY_ID))
                    .thenThrow(new SupplierInvoiceNotFoundException(99L));

            mockMvc.perform(get("/supplier-invoices/99")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /supplier-invoices/search devuelve la envoltura paginada")
        void get_search_devuelve_la_envoltura_paginada() throws Exception {
            when(searchUseCase.execute(any())).thenReturn(new PageResult<>(
                    List.of(SupplierInvoiceMother.dtoParcial(COMPANY_ID)), 1, 5, 11L, 3));

            mockMvc.perform(
                    get("/supplier-invoices/search").param("page", "1").param("pageSize", "5"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(55))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.pageSize").value(5))
                    .andExpect(jsonPath("$.totalElements").value(11))
                    .andExpect(jsonPath("$.totalPages").value(3));
        }

        @Test
        @DisplayName("GET /supplier-invoices/search sin parametros usa pagina 0, tamano 20 "
                + "y la sede del contexto")
        void get_search_por_defecto() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/supplier-invoices/search")).andExpect(status().isOk());

            verify(searchUseCase).execute(new SearchSupplierInvoicesCommand(COMPANY_ID, null,
                    BRANCH_POR_DEFECTO, null, null, null, 0, 20));
        }

        @Test
        @DisplayName("GET /supplier-invoices/search traslada todos los filtros al command")
        void get_search_traslada_los_filtros() throws Exception {
            when(searchUseCase.execute(any()))
                    .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

            mockMvc.perform(get("/supplier-invoices/search").param("supplierId", "7")
                    .param("branchId", "3").param("status", "PENDING").param("from", "2026-01-01")
                    .param("to", "2026-03-31").param("page", "2").param("pageSize", "50"))
                    .andExpect(status().isOk());

            verify(searchUseCase).execute(new SearchSupplierInvoicesCommand(COMPANY_ID, 7L,
                    BRANCH_ID, SupplierInvoiceStatus.PENDING, LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 3, 31), 2, 50));
        }

        @Test
        @DisplayName("GET /supplier-invoices/aging devuelve los tramos de antiguedad")
        void get_aging_devuelve_los_tramos() throws Exception {
            when(agingUseCase.get(any(), any(), any()))
                    .thenReturn(SupplierInvoiceMother.aging(LocalDate.of(2026, 3, 31)));

            mockMvc.perform(get("/supplier-invoices/aging").param("asOf", "2026-03-31"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.asOf").value("2026-03-31"))
                    .andExpect(jsonPath("$.suppliers[0].supplierName").value("Distribuidora Sur"))
                    .andExpect(jsonPath("$.suppliers[0].bucket.current").value(100.00))
                    .andExpect(jsonPath("$.suppliers[0].bucket.days1to30").value(200.00))
                    .andExpect(jsonPath("$.totals.total").value(300.00));

            verify(agingUseCase).get(COMPANY_ID, BRANCH_POR_DEFECTO, LocalDate.of(2026, 3, 31));
        }

        @Test
        @DisplayName("GET /supplier-invoices/aging sin asOf deja la fecha al caso de uso")
        void get_aging_sin_fecha() throws Exception {
            when(agingUseCase.get(any(), any(), any()))
                    .thenReturn(SupplierInvoiceMother.aging(LocalDate.of(2026, 3, 31)));

            mockMvc.perform(get("/supplier-invoices/aging").param("branchId", "3"))
                    .andExpect(status().isOk());

            verify(agingUseCase).get(COMPANY_ID, BRANCH_ID, null);
        }
    }

    @Nested
    @DisplayName("PUT /supplier-invoices/{id}")
    class Actualizacion {

        @Test
        @DisplayName("responde 200 y traduce el request al command con el id de la ruta")
        void responde_200_y_traduce_el_command() throws Exception {
            when(updateUseCase.execute(any()))
                    .thenReturn(SupplierInvoiceMother.dtoParcial(COMPANY_ID));

            mockMvc.perform(put("/supplier-invoices/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(55));

            verify(updateUseCase).execute(new UpdateSupplierInvoiceCommand(55L, BRANCH_ID, 7L, 31L,
                    41L, "FAC-001", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 9),
                    new BigDecimal("1000000.00"), new BigDecimal("190000.00"),
                    new BigDecimal("25000.00"), "Compra de insumos", COMPANY_ID, EMPLOYEE_ID, 1L));
        }

        @Test
        @DisplayName("sin version responde 400 y no llega al caso de uso")
        void sin_version_responde_400() throws Exception {
            mockMvc.perform(put("/supplier-invoices/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO)).andExpect(status().isBadRequest());

            verify(updateUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("editar una factura que ya tiene abonos responde 409")
        void editar_una_factura_con_abonos_responde_409() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new InvalidSupplierInvoiceStateException(
                    "Supplier invoice can only be edited while PENDING (no payments), "
                            + "current status: PARTIAL"));

            mockMvc.perform(put("/supplier-invoices/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("editar una factura de otra empresa responde 404")
        void editar_una_factura_ajena_responde_404() throws Exception {
            when(updateUseCase.execute(any())).thenThrow(new SupplierInvoiceNotFoundException(55L));

            mockMvc.perform(put("/supplier-invoices/55").contentType(MediaType.APPLICATION_JSON)
                    .content(CUERPO_VALIDO_UPDATE)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /supplier-invoices/{id}/payments")
    class Abonos {

        @Test
        @DisplayName("responde 201 con la factura y su nuevo saldo")
        void responde_201_con_el_nuevo_saldo() throws Exception {
            when(registerPaymentUseCase.execute(any()))
                    .thenReturn(SupplierInvoiceMother.dtoParcial(COMPANY_ID));

            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ABONO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PARTIAL"))
                    .andExpect(jsonPath("$.paidAmount").value(165000.00))
                    .andExpect(jsonPath("$.balance").value(1000000.00))
                    .andExpect(jsonPath("$.payments[0].id").value(88));
        }

        @Test
        @DisplayName("traduce el abono al command con el id de la ruta y el autor del contexto")
        void traduce_el_abono_al_command() throws Exception {
            when(registerPaymentUseCase.execute(any()))
                    .thenReturn(SupplierInvoiceMother.dtoParcial(COMPANY_ID));

            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ABONO));

            verify(registerPaymentUseCase)
                    .execute(new RegisterSupplierPaymentCommand(55L, new BigDecimal("165000.00"),
                            LocalDate.of(2026, 2, 1), SupplierInvoicePaymentMethod.TRANSFER,
                            "TRF-9", "Abono parcial", COMPANY_ID, EMPLOYEE_ID, 1L));
        }

        @Test
        @DisplayName("un abono de cero responde 400 y no llega al caso de uso")
        void abono_de_cero_responde_400() throws Exception {
            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":0,"paymentDate":"2026-02-01","method":"CASH","version":1}
                            """)).andExpect(status().isBadRequest());

            verify(registerPaymentUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un abono sin version responde 400: no se puede abonar a ciegas")
        void abono_sin_version_responde_400() throws Exception {
            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":165000.00,"paymentDate":"2026-02-01","method":"CASH"}
                            """)).andExpect(status().isBadRequest());

            verify(registerPaymentUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("un medio de pago que no existe responde 400")
        void medio_de_pago_desconocido_responde_400() throws Exception {
            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"amount":165000.00,"paymentDate":"2026-02-01","method":"BITCOIN",
                             "version":1}
                            """)).andExpect(status().isBadRequest());

            verify(registerPaymentUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("abonar una factura anulada responde 409")
        void abonar_una_factura_anulada_responde_409() throws Exception {
            when(registerPaymentUseCase.execute(any())).thenThrow(
                    new InvalidSupplierInvoiceStateException("Cannot pay a cancelled invoice"));

            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ABONO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("un sobrepago responde 409")
        void sobrepago_responde_409() throws Exception {
            when(registerPaymentUseCase.execute(any()))
                    .thenThrow(new InvalidSupplierInvoiceStateException(
                            "Payment 2000000.00 exceeds outstanding balance 1000000.00"));

            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ABONO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("abonar una factura inexistente responde 404")
        void abonar_una_factura_inexistente_responde_404() throws Exception {
            when(registerPaymentUseCase.execute(any()))
                    .thenThrow(new SupplierInvoiceNotFoundException(55L));

            mockMvc.perform(post("/supplier-invoices/55/payments")
                    .contentType(MediaType.APPLICATION_JSON).content(CUERPO_ABONO))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("anulacion y borrado")
    class AnulacionYBorrado {

        @Test
        @DisplayName("POST /{id}/cancel responde 200 con la factura anulada")
        void cancel_responde_200() throws Exception {
            when(cancelUseCase.execute(55L, COMPANY_ID, EMPLOYEE_ID))
                    .thenReturn(SupplierInvoiceMother.dtoAnulada(COMPANY_ID));

            mockMvc.perform(post("/supplier-invoices/55/cancel")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.balance").value(0))
                    .andExpect(jsonPath("$.payments").isEmpty());
        }

        @Test
        @DisplayName("anular una factura que ya tiene abonos responde 409")
        void anular_una_factura_con_abonos_responde_409() throws Exception {
            when(cancelUseCase.execute(55L, COMPANY_ID, EMPLOYEE_ID))
                    .thenThrow(new InvalidSupplierInvoiceStateException(
                            "Only a PENDING invoice (without payments) can be cancelled, "
                                    + "current status: PARTIAL"));

            mockMvc.perform(post("/supplier-invoices/55/cancel")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE /{id} responde 204 sin cuerpo y borra en la empresa del contexto")
        void delete_responde_204() throws Exception {
            mockMvc.perform(delete("/supplier-invoices/55")).andExpect(status().isNoContent());

            verify(deleteUseCase).execute(55L, COMPANY_ID);
        }

        @Test
        @DisplayName("DELETE de una factura inexistente responde 404")
        void delete_inexistente_responde_404() throws Exception {
            doThrow(new SupplierInvoiceNotFoundException(99L)).when(deleteUseCase).execute(99L,
                    COMPANY_ID);

            mockMvc.perform(delete("/supplier-invoices/99")).andExpect(status().isNotFound());
        }
    }
}
