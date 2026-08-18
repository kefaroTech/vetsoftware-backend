package com.vetsoftware.app.electronicdocument.infrastructure.web;

import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaPendienteConId;
import static com.vetsoftware.app.electronicdocument.testsupport.ElectronicDocumentMother.facturaValidada;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.electronicdocument.application.command.BuildElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.command.ConvertPosToInvoiceCommand;
import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.electronicdocument.application.command.IssueDebitNoteCommand;
import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.command.TransmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.BuildElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.ConvertPosToInvoiceUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.FindElectronicDocumentByAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.FindElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.IssueCreditNoteUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.IssueDebitNoteUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.ListElectronicDocumentsUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.RegisterPosSaleUseCase;
import com.vetsoftware.app.electronicdocument.application.port.in.TransmitElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.domain.CreditNoteReason;
import com.vetsoftware.app.electronicdocument.domain.DebitNoteReason;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import com.vetsoftware.app.electronicdocument.application.command.SaleLineKind;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.WebMvcSliceConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
 * Rodaja HTTP de facturacion electronica: rutas, binding y forma del JSON. La
 * autorizacion no se prueba aqui (cadena desactivada) — vive en el
 * {@code @PreAuthorize} de cada puerto de entrada. Lo que si es contrato de
 * este controller: que companyId/employeeId/branchId nunca vienen del cuerpo,
 * siempre del contexto {@code Authz}.
 */
@WebMvcTest(ElectronicDocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSliceConfig.class)
@DisplayName("ElectronicDocumentController — contrato HTTP")
class ElectronicDocumentControllerTest {

    private static final Long COMPANY_ID = WebMvcSliceConfig.COMPANY_ID;
    private static final Long EMPLOYEE_ID = WebMvcSliceConfig.EMPLOYEE_ID;
    private static final Long SEDE_RESUELTA = 7L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private Authz authz;

    @MockitoBean
    private BuildElectronicDocumentFromAccountUseCase buildUseCase;
    @MockitoBean
    private EmitElectronicDocumentFromAccountUseCase emitUseCase;
    @MockitoBean
    private RegisterPosSaleUseCase registerPosSaleUseCase;
    @MockitoBean
    private ConvertPosToInvoiceUseCase convertPosUseCase;
    @MockitoBean
    private TransmitElectronicDocumentUseCase transmitUseCase;
    @MockitoBean
    private IssueCreditNoteUseCase creditNoteUseCase;
    @MockitoBean
    private IssueDebitNoteUseCase debitNoteUseCase;
    @MockitoBean
    private FindElectronicDocumentUseCase findUseCase;
    @MockitoBean
    private FindElectronicDocumentByAccountUseCase findByAccountUseCase;
    @MockitoBean
    private ListElectronicDocumentsUseCase listUseCase;

    @BeforeEach
    void resolverLaSedeDesdeElContexto() {
        org.mockito.Mockito.lenient().when(authz.resolveAccessibleBranch(any()))
                .thenReturn(SEDE_RESUELTA);
        org.mockito.Mockito.lenient().when(authz.currentEmployeeId()).thenReturn(EMPLOYEE_ID);
    }

    @Nested
    @DisplayName("construccion y emision desde una cuenta cerrada")
    class ConstruccionYEmision {

        @Test
        @DisplayName("POST /electronic-documents/from-account sella companyId desde el contexto")
        void build_from_account_sella_company_id() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaPendienteConId(1L));
            when(buildUseCase.execute(any())).thenReturn(dto);

            mockMvc.perform(post("/electronic-documents/from-account")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"openAccountId": 100, "documentType": "FE_VENTA", "finalConsumer": false}
                                    """))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));

            verify(buildUseCase).execute(new BuildElectronicDocumentCommand(100L,
                    ElectronicDocumentType.FE_VENTA, COMPANY_ID, false));
        }

        @Test
        @DisplayName("POST /electronic-documents/emit sella companyId desde el contexto")
        void emit_sella_company_id() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(2L));
            when(emitUseCase.execute(any())).thenReturn(dto);

            mockMvc.perform(post("/electronic-documents/emit")
                    .contentType(MediaType.APPLICATION_JSON).content(
                            """
                                    {"openAccountId": 100, "documentType": "FE_VENTA", "finalConsumer": true}
                                    """))
                    .andExpect(status().isCreated());

            verify(emitUseCase).execute(new EmitElectronicDocumentCommand(100L,
                    ElectronicDocumentType.FE_VENTA, COMPANY_ID, true));
        }
    }

    @Nested
    @DisplayName("venta de POS")
    class VentaDePos {

        @Test
        @DisplayName("POST /electronic-documents/from-sale sella companyId, employeeId y sede resuelta")
        void register_pos_sale_sella_contexto() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(3L));
            when(registerPosSaleUseCase.execute(any())).thenReturn(dto);

            mockMvc.perform(post("/electronic-documents/from-sale")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {
                              "documentType": "DOC_EQUIV_POS",
                              "finalConsumer": true,
                              "customerOwnerId": null,
                              "lines": [{"kind": "PRODUCT", "refId": 5, "description": "Alimento",
                                         "quantity": 1, "unitPrice": 10000}],
                              "payments": [{"means": "EFECTIVO", "amount": 10000}],
                              "clientRequestId": "req-1",
                              "branchId": null
                            }
                            """)).andExpect(status().isCreated());

            verify(registerPosSaleUseCase).execute(new RegisterPosSaleCommand(COMPANY_ID,
                    ElectronicDocumentType.DOC_EQUIV_POS, true, null,
                    List.of(new RegisterPosSaleCommand.SaleLine(SaleLineKind.PRODUCT, 5L,
                            "Alimento", BigDecimal.ONE, new BigDecimal("10000"))),
                    List.of(new RegisterPosSaleCommand.SalePayment(PaymentMeans.EFECTIVO,
                            new BigDecimal("10000"))),
                    "req-1", EMPLOYEE_ID, SEDE_RESUELTA));
        }

        @Test
        @DisplayName("POST /electronic-documents/{id}/convert-to-invoice sella companyId")
        void convert_to_invoice_sella_company_id() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(4L));
            when(convertPosUseCase.execute(any())).thenReturn(dto);

            mockMvc.perform(post("/electronic-documents/3/convert-to-invoice"))
                    .andExpect(status().isCreated());

            verify(convertPosUseCase).execute(new ConvertPosToInvoiceCommand(3L, COMPANY_ID));
        }
    }

    @Nested
    @DisplayName("transmision y notas")
    class TransmisionYNotas {

        @Test
        @DisplayName("POST /electronic-documents/{id}/transmit sella companyId")
        void transmit_sella_company_id() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(5L));
            when(transmitUseCase.execute(any())).thenReturn(dto);

            mockMvc.perform(post("/electronic-documents/5/transmit")).andExpect(status().isOk());

            verify(transmitUseCase).execute(new TransmitElectronicDocumentCommand(5L, COMPANY_ID));
        }

        @Test
        @DisplayName("POST /electronic-documents/{id}/credit-note sella companyId y employeeId")
        void credit_note_sella_contexto() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(6L));
            when(creditNoteUseCase.execute(any())).thenReturn(dto);

            mockMvc.perform(post("/electronic-documents/1/credit-note")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason": "ANULACION"}
                            """)).andExpect(status().isCreated());

            verify(creditNoteUseCase).execute(new IssueCreditNoteCommand(1L,
                    CreditNoteReason.ANULACION, COMPANY_ID, EMPLOYEE_ID, null));
        }

        @Test
        @DisplayName("POST /electronic-documents/{id}/debit-note sella companyId y employeeId")
        void debit_note_sella_contexto() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(7L));
            when(debitNoteUseCase.execute(any())).thenReturn(dto);

            mockMvc.perform(post("/electronic-documents/1/debit-note")
                    .contentType(MediaType.APPLICATION_JSON).content("""
                            {"reason": "INTERESES", "additionalAmount": 100.00}
                            """)).andExpect(status().isCreated());

            verify(debitNoteUseCase).execute(new IssueDebitNoteCommand(1L,
                    DebitNoteReason.INTERESES, COMPANY_ID, EMPLOYEE_ID, new BigDecimal("100.00")));
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lecturas {

        @Test
        @DisplayName("GET /electronic-documents traslada la sede resuelta y usa los defectos de pagina")
        void list_all_usa_los_defectos_de_pagina() throws Exception {
            when(listUseCase.listByCompany(any(), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(PageResult.empty(0, 20));

            mockMvc.perform(get("/electronic-documents")).andExpect(status().isOk());

            verify(listUseCase).listByCompany(COMPANY_ID, SEDE_RESUELTA, null, null, 0, 20);
        }

        @Test
        @DisplayName("GET /electronic-documents/{id} devuelve el documento del caso de uso")
        void find_by_id_devuelve_el_documento() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(8L));
            when(findUseCase.findById(8L, COMPANY_ID)).thenReturn(dto);

            mockMvc.perform(get("/electronic-documents/8")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(8));
        }

        @Test
        @DisplayName("GET /electronic-documents/by-account/{id} devuelve el documento si existe")
        void find_by_account_devuelve_el_documento_si_existe() throws Exception {
            var dto = ElectronicDocumentDto.from(facturaValidada(9L));
            when(findByAccountUseCase.findByOpenAccount(100L, COMPANY_ID))
                    .thenReturn(Optional.of(dto));

            mockMvc.perform(get("/electronic-documents/by-account/100")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(9));
        }

        @Test
        @DisplayName("GET /electronic-documents/by-account/{id} responde 404 cuando la cuenta no facturo")
        void find_by_account_responde_404_sin_documento() throws Exception {
            when(findByAccountUseCase.findByOpenAccount(101L, COMPANY_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/electronic-documents/by-account/101"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /electronic-documents/{id} responde 404 con un documento inexistente")
        void find_by_id_responde_404_documento_inexistente() throws Exception {
            when(findUseCase.findById(999L, COMPANY_ID))
                    .thenThrow(new ElectronicDocumentNotFoundException(999L));

            mockMvc.perform(get("/electronic-documents/999")).andExpect(status().isNotFound());
        }
    }
}
